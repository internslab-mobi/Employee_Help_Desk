package xyz.mobi.employeehelpdesk.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.mobi.employeehelpdesk.dto.auth.*;
import xyz.mobi.employeehelpdesk.entity.Employee;
import xyz.mobi.employeehelpdesk.entity.PasswordResetOtp;
import xyz.mobi.employeehelpdesk.entity.RefreshToken;
import xyz.mobi.employeehelpdesk.entity.enums.EmploymentStatus;
import xyz.mobi.employeehelpdesk.entity.enums.UserRole;
import xyz.mobi.employeehelpdesk.exception.BadRequestException;
import xyz.mobi.employeehelpdesk.exception.ResourceNotFoundException;
import xyz.mobi.employeehelpdesk.exception.UnauthorizedException;
import xyz.mobi.employeehelpdesk.repository.EmployeeRepository;
import xyz.mobi.employeehelpdesk.repository.PasswordResetOtpRepository;
import xyz.mobi.employeehelpdesk.repository.RefreshTokenRepository;
import xyz.mobi.employeehelpdesk.security.JwtTokenProvider;
import xyz.mobi.employeehelpdesk.security.UserPrincipal;
import xyz.mobi.employeehelpdesk.service.AuthService;
import xyz.mobi.employeehelpdesk.service.helperservice.EmailService;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final EmployeeRepository employeeRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetOtpRepository passwordResetOtpRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;

    @Override
    @Transactional
    public LoginResponseDTO login(LoginRequestDTO request) {
        if (request == null || request.email() == null || request.password() == null) {
            log.warn("Login failed: missing email or password");
            throw new UnauthorizedException("Invalid email or password");
        }

        Employee employee = employeeRepository.findByEmail(request.email().trim())
                .orElseThrow(() -> {
                    log.warn("Login failed for email={}: invalid email or password", request.email());
                    return new UnauthorizedException("Invalid email or password");
                });

        if (employee.getPasswordHash() == null || !passwordEncoder.matches(request.password(), employee.getPasswordHash())) {
            log.warn("Login failed for email={}: invalid email or password", request.email());
            throw new UnauthorizedException("Invalid email or password");
        }

        if (employee.getEmploymentStatus() != EmploymentStatus.ACTIVE) {
            log.warn("Login failed for email={}, employeeId={}: account is not active", employee.getEmail(), employee.getId());
            throw new UnauthorizedException("Employee account is not active");
        }

        UserRole role = employee.getRole() != null ? employee.getRole() : UserRole.EMPLOYEE;
        String timezone = employee.getTimezone() != null ? employee.getTimezone() : "UTC";
        String accessToken = jwtTokenProvider.generateToken(employee.getId(), role, timezone);
        long expiresIn = jwtTokenProvider.getExpirationMs();

        // Generate Refresh Token (45 min lifetime, max 3 usages)
        String refreshTokenStr = UUID.randomUUID().toString();
        Instant refreshExpiresAt = Instant.now().plusMillis(jwtTokenProvider.getRefreshExpirationMs());

        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenStr)
                .employee(employee)
                .usageCount(0)
                .maxUses(3)
                .expiresAt(refreshExpiresAt)
                .revoked(false)
                .createdAt(Instant.now())
                .build();

        refreshTokenRepository.save(refreshToken);

        log.info("Login successful: employeeId={}, role={}", employee.getId(), role);

        return new LoginResponseDTO(
                accessToken,
                refreshTokenStr,
                "Bearer",
                expiresIn,
                employee.getId(),
                role
        );
    }

    @Override
    @Transactional
    public TokenRefreshResponseDTO refreshToken(RefreshTokenRequestDTO request) {
        if (request == null || request.refreshToken() == null || request.refreshToken().isBlank()) {
            log.warn("Token refresh rejected: missing refresh token");
            throw new UnauthorizedException("Refresh token is required");
        }

        String tokenStr = request.refreshToken().trim();
        Instant now = Instant.now();

        // Atomically increment usage count and revoke if limit reached
        Integer updated = refreshTokenRepository.incrementUsageIfValid(tokenStr, now);
        if (updated == null || updated == 0) {
            log.warn("Token refresh failed: invalid, expired, or over-used token");
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        RefreshToken refreshToken = refreshTokenRepository.findByToken(tokenStr)
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired refresh token"));

        Employee employee = refreshToken.getEmployee();
        if (employee == null || employee.getEmploymentStatus() != EmploymentStatus.ACTIVE) {
            log.warn("Token refresh failed for employeeId={}: account is not active", employee != null ? employee.getId() : null);
            throw new UnauthorizedException("Employee account is not active");
        }

        UserRole role = employee.getRole() != null ? employee.getRole() : UserRole.EMPLOYEE;
        String timezone = employee.getTimezone() != null ? employee.getTimezone() : "UTC";
        String newAccessToken = jwtTokenProvider.generateToken(employee.getId(), role, timezone);

        log.info("Token refreshed successfully: employeeId={}, role={}", employee.getId(), role);

        return new TokenRefreshResponseDTO(
                newAccessToken,
                refreshToken.getToken(),
                "Bearer",
                jwtTokenProvider.getExpirationMs()
        );
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequestDTO request) {
        if (request == null || request.currentPassword() == null || request.newPassword() == null || request.confirmNewPassword() == null) {
            throw new BadRequestException("All password fields are required");
        }

        if (request.newPassword().isBlank()) {
            throw new BadRequestException("New password cannot be empty");
        }

        if (!request.newPassword().equals(request.confirmNewPassword())) {
            throw new BadRequestException("New password and confirm password do not match");
        }

        Long currentEmployeeId = getCurrentEmployeeId();
        Employee employee = employeeRepository.findById(currentEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + currentEmployeeId));

        if (!passwordEncoder.matches(request.currentPassword(), employee.getPasswordHash())) {
            log.warn("Password change failed for employeeId={}: current password incorrect", currentEmployeeId);
            throw new BadRequestException("Current password is incorrect");
        }

        if (passwordEncoder.matches(request.newPassword(), employee.getPasswordHash())) {
            throw new BadRequestException("New password cannot be the same as current password");
        }

        employee.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        employeeRepository.save(employee);

        // Invalidate all active refresh tokens on password change
        refreshTokenRepository.deleteByEmployeeId(employee.getId());

        log.info("Password changed successfully: employeeId={}", employee.getId());
    }

    @Override
    @Transactional
    public void requestForgotPasswordOtp(ForgotPasswordOtpRequestDTO request) {
        if (request == null || request.email() == null || request.email().isBlank()) {
            throw new BadRequestException("Email is required");
        }

        String email = request.email().trim();
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Password reset OTP request failed: no active employee found for email={}", email);
                    return new ResourceNotFoundException("No active employee found with email: " + email);
                });

        if (employee.getEmploymentStatus() != EmploymentStatus.ACTIVE) {
            log.warn("Password reset OTP request failed for email={}, employeeId={}: account not active", email, employee.getId());
            throw new BadRequestException("Employee account is not active");
        }

        // Generate 6-digit numeric OTP
        SecureRandom random = new SecureRandom();
        String otp = String.format("%06d", random.nextInt(1000000));

        // Invalidate/replace existing OTPs for this email
        passwordResetOtpRepository.deleteByEmail(email);

        PasswordResetOtp resetOtp = PasswordResetOtp.builder()
                .email(email)
                .otpHash(passwordEncoder.encode(otp))
                .otpExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .otpAttemptCount(0)
                .otpVerified(false)
                .resetTokenHash(null)
                .resetTokenExpiresAt(null)
                .resetTokenUsed(false)
                .build();

        passwordResetOtpRepository.save(resetOtp);

        emailService.sendNotificationEmail(
                email,
                "Password Reset OTP",
                "Your OTP for password reset is: " + otp + ". This OTP is valid for 10 minutes."
        );

        log.info("Password reset OTP generated and sent to email={}", email);
    }

    @Override
    @Transactional
    public VerifyOtpResponseDTO verifyOtp(VerifyOtpRequestDTO request) {
        if (request == null || request.email() == null || request.otp() == null) {
            throw new BadRequestException("Email and OTP are required");
        }

        String email = request.email().trim();
        String otp = request.otp().trim();

        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No active employee found with email: " + email));

        if (employee.getEmploymentStatus() != EmploymentStatus.ACTIVE) {
            throw new BadRequestException("Employee account is not active");
        }

        PasswordResetOtp resetOtp = passwordResetOtpRepository
                .findTopByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new BadRequestException("No OTP request found for this email. Please request a new OTP."));

        if (resetOtp.isOtpVerified()) {
            throw new BadRequestException("OTP has already been verified");
        }

        if (resetOtp.getOtpExpiresAt().isBefore(Instant.now())) {
            log.warn("OTP verification rejected for email={}: OTP expired", email);
            throw new BadRequestException("OTP has expired. Please request a new OTP.");
        }

        if (resetOtp.getOtpAttemptCount() >= 3) {
            log.warn("OTP verification rejected for email={}: maximum attempts exceeded", email);
            throw new BadRequestException("Maximum OTP attempts exceeded. Please request a new OTP.");
        }

        if (!passwordEncoder.matches(otp, resetOtp.getOtpHash())) {
            int newAttemptCount = resetOtp.getOtpAttemptCount() + 1;
            resetOtp.setOtpAttemptCount(newAttemptCount);
            passwordResetOtpRepository.save(resetOtp);

            log.warn("OTP verification attempt failed for email={}, attempts={}", email, newAttemptCount);

            if (newAttemptCount >= 3) {
                throw new BadRequestException("Maximum OTP attempts exceeded. Please request a new OTP.");
            }
            int remainingAttempts = 3 - newAttemptCount;
            throw new BadRequestException("Invalid OTP. " + remainingAttempts + " attempt(s) remaining.");
        }

        // Mark OTP verification as successful
        resetOtp.setOtpVerified(true);

        // Generate cryptographically secure, short-lived reset token
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        String resetToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        // Store only the hash of the reset token
        resetOtp.setResetTokenHash(passwordEncoder.encode(resetToken));
        resetOtp.setResetTokenExpiresAt(Instant.now().plus(15, ChronoUnit.MINUTES));
        resetOtp.setResetTokenUsed(false);
        passwordResetOtpRepository.save(resetOtp);

        log.info("Password reset OTP verified successfully for email={}, employeeId={}", email, employee.getId());

        return new VerifyOtpResponseDTO(resetToken);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequestDTO request) {
        if (request == null || request.email() == null || request.resetToken() == null
                || request.newPassword() == null || request.confirmNewPassword() == null) {
            throw new BadRequestException("All fields are required");
        }

        if (!request.newPassword().equals(request.confirmNewPassword())) {
            throw new BadRequestException("New password and confirm password do not match");
        }

        if (request.newPassword().isBlank()) {
            throw new BadRequestException("New password cannot be empty");
        }

        String email = request.email().trim();
        String resetToken = request.resetToken().trim();

        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No active employee found with email: " + email));

        if (employee.getEmploymentStatus() != EmploymentStatus.ACTIVE) {
            throw new BadRequestException("Employee account is not active");
        }

        PasswordResetOtp resetOtp = passwordResetOtpRepository
                .findTopByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new BadRequestException("Invalid or expired password reset token"));

        if (!resetOtp.isOtpVerified()) {
            log.warn("Password reset rejected for email={}: OTP was not verified", email);
            throw new BadRequestException("OTP verification required before resetting password");
        }

        if (resetOtp.isResetTokenUsed()) {
            log.warn("Password reset rejected for email={}: reset token already used", email);
            throw new BadRequestException("Reset token has already been used");
        }

        if (resetOtp.getResetTokenExpiresAt() == null || resetOtp.getResetTokenExpiresAt().isBefore(Instant.now())) {
            log.warn("Password reset rejected for email={}: reset token expired", email);
            throw new BadRequestException("Reset token has expired");
        }

        if (resetOtp.getResetTokenHash() == null || !passwordEncoder.matches(resetToken, resetOtp.getResetTokenHash())) {
            log.warn("Password reset rejected for email={}: invalid reset token", email);
            throw new BadRequestException("Invalid password reset token");
        }

        if (employee.getPasswordHash() != null && passwordEncoder.matches(request.newPassword(), employee.getPasswordHash())) {
            throw new BadRequestException("New password cannot be the same as current password");
        }

        employee.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        employeeRepository.save(employee);

        // Invalidate reset token immediately after successful password reset
        resetOtp.setResetTokenUsed(true);
        passwordResetOtpRepository.save(resetOtp);

        // Invalidate active refresh tokens
        refreshTokenRepository.deleteByEmployeeId(employee.getId());

        log.info("Password reset completed successfully for email={}, employeeId={}", email, employee.getId());
    }

    @Override
    public Long getCurrentEmployeeId() {
        return getAuthenticatedPrincipal().getEmployeeId();
    }

    @Override
    public UserRole getCurrentUserRole() {
        return getAuthenticatedPrincipal().getRole();
    }

    @Override
    public String getCurrentUserEmail() {
        return getAuthenticatedPrincipal().getEmail();
    }

    @Override
    public String getCurrentUserTimezone() {
        return getAuthenticatedPrincipal().getTimezone();
    }

    @Override
    public java.time.ZoneId getCurrentUserZoneId() {
        return getAuthenticatedPrincipal().getZoneId();
    }

    private UserPrincipal getAuthenticatedPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new UnauthorizedException("User is not authenticated");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal;
        }

        throw new UnauthorizedException("Invalid authentication principal");
    }
}
