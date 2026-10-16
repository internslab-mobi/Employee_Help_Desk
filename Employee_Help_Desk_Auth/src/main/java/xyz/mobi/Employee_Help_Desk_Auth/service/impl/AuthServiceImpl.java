package xyz.mobi.Employee_Help_Desk_Auth.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.mobi.Employee_Help_Desk_Auth.client.NotificationClient;
import xyz.mobi.Employee_Help_Desk_Auth.dto.auth.*;
import xyz.mobi.Employee_Help_Desk_Auth.entity.Employee;
import xyz.mobi.Employee_Help_Desk_Auth.entity.PasswordResetOtp;
import xyz.mobi.Employee_Help_Desk_Auth.entity.RefreshToken;
import xyz.mobi.Employee_Help_Desk_Auth.entity.enums.EmploymentStatus;
import xyz.mobi.Employee_Help_Desk_Auth.entity.enums.UserRole;
import xyz.mobi.Employee_Help_Desk_Auth.exception.*;
import xyz.mobi.Employee_Help_Desk_Auth.repository.EmployeeRepository;
import xyz.mobi.Employee_Help_Desk_Auth.repository.PasswordResetOtpRepository;
import xyz.mobi.Employee_Help_Desk_Auth.repository.RefreshTokenRepository;
import xyz.mobi.Employee_Help_Desk_Auth.security.JwtTokenProvider;
import xyz.mobi.Employee_Help_Desk_Auth.security.UserPrincipal;
import xyz.mobi.Employee_Help_Desk_Auth.service.AuthService;
import xyz.mobi.Employee_Help_Desk_Auth.service.PasswordResetOtpPersistenceService;

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
    private final NotificationClient notificationClient;
    private final PasswordResetOtpPersistenceService otpPersistenceService;

    private static final String PASSWORD_PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{8,}$";

    @Override
    @Transactional
    public LoginResponseDTO login(LoginRequestDTO request) {

        if (request == null
                || request.email() == null
                || request.password() == null) {

            log.warn("Login failed: missing email or password");
            throw new UnauthorizedException("Invalid email or password");
        }

        Employee employee = employeeRepository
                .findByEmail(request.email().trim())
                .orElseThrow(() -> {
                    log.warn("Login failed for email={}: invalid email or password", request.email());
                    return new UnauthorizedException("Invalid email or password");
                });

        if (employee.getPasswordHash() == null
                || !passwordEncoder.matches(request.password(), employee.getPasswordHash())) {

            log.warn("Login failed for email={}: invalid email or password", request.email());
            throw new UnauthorizedException("Invalid email or password");
        }

        if (employee.getEmploymentStatus() != EmploymentStatus.ACTIVE) {
            log.warn("Login failed for email={}, status={}: account is inactive", request.email(), employee.getEmploymentStatus());
            throw new UnauthorizedException("Employee account is not active");
        }

        UserRole role = employee.getRole() != null ? employee.getRole() : UserRole.EMPLOYEE;
        String timezone = employee.getTimezone() != null ? employee.getTimezone() : "UTC";

        String accessToken = jwtTokenProvider.generateToken(
                employee.getId(),
                role,
                timezone
        );

        String refreshTokenStr = UUID.randomUUID().toString();
        Instant refreshTokenExpiresAt = Instant.now().plusMillis(jwtTokenProvider.getRefreshExpirationMs());

        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenStr)
                .employee(employee)
                .usageCount(0)
                .maxUses(3)
                .expiresAt(refreshTokenExpiresAt)
                .revoked(false)
                .createdAt(Instant.now())
                .build();

        refreshTokenRepository.save(refreshToken);

        log.info("Login successful: employeeId={}, role={}", employee.getId(), role);

        return new LoginResponseDTO(
                accessToken,
                refreshTokenStr,
                "Bearer",
                jwtTokenProvider.getExpirationMs(),
                employee.getId(),
                role
        );
    }

    @Override
    @Transactional
    public TokenRefreshResponseDTO refreshToken(RefreshTokenRequestDTO request) {

        if (request == null
                || request.refreshToken() == null
                || request.refreshToken().isBlank()) {

            throw new UnauthorizedException("Refresh token is required");
        }

        String tokenStr = request.refreshToken().trim();
        Instant now = Instant.now();

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

        String newAccessToken = jwtTokenProvider.generateToken(
                employee.getId(),
                role,
                timezone
        );

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

        if (request == null
                || request.currentPassword() == null
                || request.newPassword() == null
                || request.confirmNewPassword() == null) {

            throw new BadRequestException("All password fields are required");
        }

        if (request.newPassword().isBlank()) {
            throw new BadRequestException("New password cannot be empty");
        }

        if (!request.newPassword().equals(request.confirmNewPassword())) {
            throw new BadRequestException("New password and confirm password do not match");
        }

        validatePassword(request.newPassword());

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

        refreshTokenRepository.deleteByEmployeeId(employee.getId());

        log.info("Password changed successfully: employeeId={}", employee.getId());
    }

    @Override
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

        SecureRandom random = new SecureRandom();
        String otp = String.format("%06d", random.nextInt(1_000_000));

        PasswordResetOtp savedOtp = otpPersistenceService.saveOtp(email, otp);

        try {
            notificationClient.sendNotificationEmail(email, "Password reset OTP",
                    "Your OTP for the password reset is: " + otp + ". This OTP is valid for only 2 minutes.");
            log.info("Password reset OTP notification accepted for email={}", email);
        }
        catch (RestClientException ex){
            try{
                otpPersistenceService.invalidateOtp(savedOtp.getId());
            }
            catch (Exception cleanupException){
                log.error("Failed to invalidate OTP after notification failure, otpId {}", savedOtp.getId(), cleanupException);
            }
            throw new NotificationException("Unable to send OTP email. Please try again");
        }
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
            throw new BadRequestException("Maximum retry exceeded");
        }

        if (!passwordEncoder.matches(otp, resetOtp.getOtpHash())) {
            int newAttemptCount = resetOtp.getOtpAttemptCount() + 1;
            resetOtp.setOtpAttemptCount(newAttemptCount);
            passwordResetOtpRepository.save(resetOtp);

            log.warn("OTP verification attempt failed for email={}, attempts={}", email, newAttemptCount);

            if (newAttemptCount >= 3) {
                throw new BadRequestException("Maximum retry exceeded");
            }

            int remainingAttempts = 3 - newAttemptCount;
            throw new BadRequestException("Invalid OTP. " + remainingAttempts + " attempt(s) remaining.");
        }

        resetOtp.setOtpVerified(true);

        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        String resetToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

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

        if (request == null
                || request.email() == null
                || request.resetToken() == null
                || request.newPassword() == null
                || request.confirmNewPassword() == null) {

            throw new BadRequestException("All fields are required");
        }

        if (!request.newPassword().equals(request.confirmNewPassword())) {
            throw new BadRequestException("New password and confirm password do not match");
        }

        if (request.newPassword().isBlank()) {
            throw new BadRequestException("New password cannot be empty");
        }

        validatePassword(request.newPassword());

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

        if (resetOtp.getResetTokenExpiresAt() == null
                || resetOtp.getResetTokenExpiresAt().isBefore(Instant.now())) {

            log.warn("Password reset rejected for email={}: reset token expired", email);
            throw new BadRequestException("Reset token has expired");
        }

        if (resetOtp.getResetTokenHash() == null
                || !passwordEncoder.matches(resetToken, resetOtp.getResetTokenHash())) {

            log.warn("Password reset rejected for email={}: invalid reset token", email);
            throw new BadRequestException("Invalid password reset token");
        }

        if (employee.getPasswordHash() != null
                && passwordEncoder.matches(request.newPassword(), employee.getPasswordHash())) {

            throw new BadRequestException("New password cannot be the same as current password");
        }

        employee.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        employeeRepository.save(employee);

        resetOtp.setResetTokenUsed(true);
        passwordResetOtpRepository.save(resetOtp);

        refreshTokenRepository.deleteByEmployeeId(employee.getId());

        log.info("Password reset completed successfully for email={}, employeeId={}", email, employee.getId());
    }

    private void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new BadRequestException("Password cannot be empty");
        }

        if (!password.matches(PASSWORD_PATTERN)) {
            throw new BadRequestException(
                    "Password must be at least 8 characters long and contain "
                            + "at least one uppercase letter, "
                            + "one lowercase letter, "
                            + "one number, "
                            + "and one special character"
            );
        }
    }

    @Override
    public Long getCurrentEmployeeId() {
        return getAuthenticatedPrincipal().getEmployeeId();
    }

    private UserPrincipal getAuthenticatedPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {

            throw new UnauthorizedException("User is not authenticated");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal;
        }

        throw new UnauthorizedException("Invalid authentication principal");
    }

}
