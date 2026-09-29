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
    public LoginResponse login(LoginRequest request) {
        if (request == null || request.email() == null || request.password() == null) {
            throw new UnauthorizedException("Invalid email or password");
        }

        Employee employee = employeeRepository.findByEmail(request.email().trim())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (employee.getPasswordHash() == null || !passwordEncoder.matches(request.password(), employee.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        if (employee.getEmploymentStatus() != EmploymentStatus.ACTIVE) {
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

        return new LoginResponse(
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
    public TokenRefreshResponse refreshToken(RefreshTokenRequest request) {
        if (request == null || request.refreshToken() == null || request.refreshToken().isBlank()) {
            throw new UnauthorizedException("Refresh token is required");
        }

        String tokenStr = request.refreshToken().trim();
        Instant now = Instant.now();

        // Atomically increment usage count and revoke if limit reached
        int updated = refreshTokenRepository.incrementUsageIfValid(tokenStr, now);
        if (updated == 0) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        RefreshToken refreshToken = refreshTokenRepository.findByToken(tokenStr)
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired refresh token"));

        Employee employee = refreshToken.getEmployee();
        if (employee == null || employee.getEmploymentStatus() != EmploymentStatus.ACTIVE) {
            throw new UnauthorizedException("Employee account is not active");
        }

        UserRole role = employee.getRole() != null ? employee.getRole() : UserRole.EMPLOYEE;
        String timezone = employee.getTimezone() != null ? employee.getTimezone() : "UTC";
        String newAccessToken = jwtTokenProvider.generateToken(employee.getId(), role, timezone);

        return new TokenRefreshResponse(
                newAccessToken,
                refreshToken.getToken(),
                "Bearer",
                jwtTokenProvider.getExpirationMs()
        );
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
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
            throw new BadRequestException("Current password is incorrect");
        }

        if (passwordEncoder.matches(request.newPassword(), employee.getPasswordHash())) {
            throw new BadRequestException("New password cannot be the same as current password");
        }

        employee.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        employeeRepository.save(employee);

        // Invalidate all active refresh tokens on password change
        refreshTokenRepository.deleteByEmployeeId(employee.getId());
    }

    @Override
    @Transactional
    public void requestForgotPasswordOtp(ForgotPasswordOtpRequest request) {
        if (request == null || request.email() == null || request.email().isBlank()) {
            throw new BadRequestException("Email is required");
        }

        String email = request.email().trim();
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No active employee found with email: " + email));

        if (employee.getEmploymentStatus() != EmploymentStatus.ACTIVE) {
            throw new BadRequestException("Employee account is not active");
        }

        // Generate 6-digit numeric OTP
        SecureRandom random = new SecureRandom();
        String otp = String.format("%06d", random.nextInt(1000000));

        // Delete existing OTPs for this email
        passwordResetOtpRepository.deleteByEmail(email);

        PasswordResetOtp resetOtp = PasswordResetOtp.builder()
                .email(email)
                .otp(passwordEncoder.encode(otp))
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .used(false)
                .createdAt(Instant.now())
                .build();

        passwordResetOtpRepository.save(resetOtp);

        emailService.sendNotificationEmail(
                email,
                "Password Reset OTP",
                "Your OTP for password reset is: " + otp + ". This OTP is valid for 10 minutes."
        );
    }

    @Override
    @Transactional
    public void resetPasswordWithOtp(ResetPasswordWithOtpRequest request) {
        if (request == null || request.email() == null || request.otp() == null
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
        String otp = request.otp().trim();

        PasswordResetOtp resetOtp = passwordResetOtpRepository
                .findTopByEmailAndUsedFalseOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new BadRequestException("Invalid or expired OTP"));

        if (resetOtp.isUsed() || resetOtp.getExpiresAt().isBefore(Instant.now())
                || !passwordEncoder.matches(otp, resetOtp.getOtp())) {
            throw new BadRequestException("Invalid or expired OTP");
        }

        // Atomically mark OTP as used to prevent concurrent replay
        int updated = passwordResetOtpRepository.consumeOtpIfValid(resetOtp.getId(), Instant.now());
        if (updated == 0) {
            throw new BadRequestException("Invalid or expired OTP");
        }

        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with email: " + email));

        employee.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        employeeRepository.save(employee);

        // Invalidate active refresh tokens
        refreshTokenRepository.deleteByEmployeeId(employee.getId());
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
