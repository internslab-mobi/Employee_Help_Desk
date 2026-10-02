package com.divya.helpdesk.service.impl;

import com.divya.helpdesk.dto.auth.*;
import com.divya.helpdesk.entity.HDEmployeeEntity;
import com.divya.helpdesk.entity.HDRefreshTokenEntity;
import com.divya.helpdesk.exception.AccessDeniedException;
import com.divya.helpdesk.exception.BadRequestException;
import com.divya.helpdesk.exception.InvalidOtpException;
import com.divya.helpdesk.exception.ResourceNotFoundException;
import com.divya.helpdesk.repository.HDEmployeeRepository;
import com.divya.helpdesk.repository.HDRefreshTokenRepository;
import com.divya.helpdesk.service.AuthService;
import com.divya.helpdesk.service.EmailService;
import com.divya.helpdesk.service.JWTService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final HDEmployeeRepository employeeRepository;
    private final HDRefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTService jwtService;
    private final EmailService emailService;

    private static final int MAX_REFRESH_COUNT = 3;

    @Value("${jwt.refresh-token-expiration:604800000}")
    private long refreshTokenExpirationMs;

    @Value("${otp.expiration-minutes:2}")
    private long otpExpirationMinutes;

    @Value("${otp.max-attempts:3}")
    private int otpMaxAttempts;

    private final SecureRandom secureRandom = new SecureRandom();

    private static class OtpState {
        private final String otp;
        private final Instant expiresAt;
        private final AtomicInteger failedAttempts;

        public OtpState(String otp, Instant expiresAt) {
            this.otp = otp;
            this.expiresAt = expiresAt;
            this.failedAttempts = new AtomicInteger(0);
        }

        public String getOtp() {
            return otp;
        }

        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }

        public int incrementFailedAttempts() {
            return failedAttempts.incrementAndGet();
        }

        public int getFailedAttempts() {
            return failedAttempts.get();
        }
    }

    private final Map<String, OtpState> otpStore = new ConcurrentHashMap<>();
    private final Map<String, String> resetTokenStore = new ConcurrentHashMap<>();

    private String generateSecureRandomToken() {
        byte[] randomBytes = new byte[64];
        secureRandom.nextBytes(randomBytes);
        return HexFormat.of().formatHex(randomBytes);
    }

    // LOGIN
    @Transactional
    @Override
    public LoginResponseDTO login(LoginRequestDTO request) {
        if (request == null || request.getEmail() == null || request.getPassword() == null) {
            throw new BadRequestException("Email and password are required");
        }

        String email = request.getEmail().trim().toLowerCase();
        HDEmployeeEntity employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!Boolean.TRUE.equals(employee.getEnabled())) {
            throw new AccessDeniedException("Account is disabled");
        }

        // Check password.
        if (!passwordEncoder.matches(request.getPassword(), employee.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        UserDetails userDetails = User.builder()
                .username(employee.getEmail())
                .password(employee.getPassword())
                .authorities("ROLE_" + employee.getRole().name())
                .build();
        String accessToken = jwtService.generateToken(userDetails);

        // Generate and save traditional opaque database-backed refresh token (refreshCount = 0)
        String rawRefreshToken = generateSecureRandomToken();
        HDRefreshTokenEntity refreshToken = new HDRefreshTokenEntity();
        refreshToken.setEmployee(employee);
        refreshToken.setToken(rawRefreshToken);
        refreshToken.setRefreshCount(0);
        refreshToken.setExpiresAt(Instant.now().plusMillis(refreshTokenExpirationMs));
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);

        boolean mustChangePassword = !Boolean.TRUE.equals(employee.getActivated());

        return LoginResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .employeeId(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .email(employee.getEmail())
                .fullName(employee.getFirstName() + " " + employee.getLastName())
                .role(employee.getRole())
                .departmentId(employee.getDepartment() != null ? employee.getDepartment().getId() : null)
                .departmentName(employee.getDepartment() != null ? employee.getDepartment().getName() : null)
                .timezone(employee.getTimezone())
                .mustChangePassword(mustChangePassword)
                .activated(employee.getActivated())
                .build();
    }

    // FORGOT PASSWORD - SEND OTP
    @Override
    public String forgotPassword(ForgotPasswordRequestDTO request) {
        if (request == null || request.getEmail() == null || request.getEmail().isBlank()) {
            throw new BadRequestException("Email is required");
        }

        String email = request.getEmail().trim().toLowerCase();
        HDEmployeeEntity employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with email: " + request.getEmail()));

        /*
         * Disabled employees cannot reset password.
         */
        if (!Boolean.TRUE.equals(employee.getEnabled())) {
            throw new AccessDeniedException("Account is disabled");
        }

        /*
         * Employee should have activated the account first.
         */
        if (!Boolean.TRUE.equals(employee.getActivated())) {
            throw new AccessDeniedException("Account is not activated");
        }
        /*
         * Generate six-digit OTP with expiration window.
         */
        String otp = String.valueOf((int) (Math.random() * 900000) + 100000);
        Instant expiresAt = Instant.now().plus(otpExpirationMinutes, ChronoUnit.MINUTES);
        otpStore.put(email, new OtpState(otp, expiresAt));

        emailService.sendOtpEmail(employee.getEmail(), otp);
        return "OTP sent successfully";
    }

    // VERIFY OTP
    @Override
    public VerifyOtpResponseDTO verifyOtp(VerifyOtpRequestDTO request) {
        if (request == null || request.getEmail() == null || request.getOtp() == null) {
            throw new BadRequestException("Email and OTP are required");
        }

        String email = request.getEmail().trim().toLowerCase();
        OtpState otpState = otpStore.get(email);

        if (otpState == null) {
            throw new InvalidOtpException("OTP not found or expired");
        }

        /*
         * Check expiration before accepting the OTP or counting failed attempts.
         */
        if (otpState.isExpired()) {
            otpStore.remove(email);
            throw new InvalidOtpException("OTP has expired. Please restart the forgot-password process.");
        }

        if (otpState.getOtp().equals(request.getOtp().trim())) {
            /*
              * OTP is correct.
              * Generate a temporary reset token.
              */
            String resetToken = UUID.randomUUID().toString();
            resetTokenStore.put(email, resetToken);
            /*
              * OTP can no longer be reused.
              */
            otpStore.remove(email);

            return VerifyOtpResponseDTO.builder()
                    .email(request.getEmail())
                    .resetToken(resetToken)
                    .message("OTP verified successfully")
                    .build();
        }

        int attempts = otpState.incrementFailedAttempts();
        if (attempts >= otpMaxAttempts) {
            otpStore.remove(email);
            throw new InvalidOtpException("Maximum OTP retry exceeded. Please restart the forgot-password process.");
        } else {
            throw new InvalidOtpException("Invalid OTP. Please retry.");
        }
    }

    // RESET PASSWORD
    @Override
    public String resetPassword(ResetPasswordRequestDTO request) {
        if (request == null || request.getEmail() == null || request.getResetToken() == null) {
            throw new BadRequestException("Email and reset token are required");
        }
        String email = request.getEmail().trim().toLowerCase();

        /*
         * Check whether OTP verification generated a valid reset token.
         */
        String storedToken = resetTokenStore.get(email);
        if (storedToken == null) {
            throw new BadRequestException("Reset token not found or expired");
        }
        if (!storedToken.equals(request.getResetToken().trim())) {
            throw new BadRequestException("Invalid reset token");
        }
        /*
         * Validate password confirmation.
         */
        if (request.getNewPassword() == null || !request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("New password and confirm password do not match");
        }
        HDEmployeeEntity employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with email: " + request.getEmail()));
        /*
         * Update password and set activated to true.
         */
        employee.setPassword(passwordEncoder.encode(request.getNewPassword()));
        employee.setActivated(true);
        employeeRepository.save(employee);
        /*
         * Reset token becomes invalid after successful password reset.
         */
        resetTokenStore.remove(email);
        return "Password reset successfully";
    }

    // CHANGE PASSWORD
    @Override
    public String changePassword(String email, ChangePasswordRequestDTO request) {
        if (email == null || request == null) {
            throw new BadRequestException("Request and email are required");
        }
        HDEmployeeEntity employee = employeeRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with email: " + email));
        /*
         * Verify current password.
         */
        if (!passwordEncoder.matches(request.getCurrentPassword(), employee.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }
        /*
         * Validate new password confirmation.
         */
        if (request.getNewPassword() == null || !request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("New password and confirm password do not match");
        }
        /*
         * Update password and set activated to true.
         */
        employee.setPassword(passwordEncoder.encode(request.getNewPassword()));
        employee.setActivated(true);
        employeeRepository.save(employee);
        return "Password changed successfully";
    }

    // REFRESH TOKEN
    @Transactional
    @Override
    public LoginResponseDTO refreshToken(RefreshTokenRequestDTO request) {
        if (request == null || request.getToken() == null || request.getToken().isBlank()) {
            throw new BadRequestException("Token must not be empty");
        }

        String rawToken = request.getToken().trim();
        if (rawToken.startsWith("Bearer ")) {
            rawToken = rawToken.substring(7).trim();
        }

        // Database lookup with pessimistic write lock to prevent race conditions on concurrent refresh
        HDRefreshTokenEntity tokenRecord = refreshTokenRepository.findByTokenForUpdate(rawToken)
                .orElseThrow(() -> new AccessDeniedException("Invalid refresh token"));

        if (Boolean.TRUE.equals(tokenRecord.getRevoked())) {
            throw new AccessDeniedException("Refresh token has been revoked");
        }

        if (tokenRecord.getExpiresAt().isBefore(Instant.now())) {
            throw new AccessDeniedException("Refresh token has expired");
        }

        // Enforce maximum 3 refresh operations per refresh-token chain/session
        int currentCount = tokenRecord.getRefreshCount() != null ? tokenRecord.getRefreshCount() : 0;
        if (currentCount >= MAX_REFRESH_COUNT) {
            throw new AccessDeniedException("Maximum token refresh limit exceeded. Please log in again.");
        }

        HDEmployeeEntity employee = tokenRecord.getEmployee();
        if (employee == null) {
            throw new AccessDeniedException("Employee not found for refresh token");
        }

        if (!Boolean.TRUE.equals(employee.getEnabled())) {
            throw new AccessDeniedException("Account is disabled");
        }

        if (!Boolean.TRUE.equals(employee.getActivated())) {
            throw new AccessDeniedException("Account is not activated");
        }

        // Refresh token rotation: revoke the old token
        tokenRecord.setRevoked(true);
        refreshTokenRepository.save(tokenRecord);

        // Generate new JWT access token
        UserDetails userDetails = User.builder()
                .username(employee.getEmail())
                .password(employee.getPassword() != null ? employee.getPassword() : "")
                .authorities("ROLE_" + employee.getRole().name())
                .build();
        String newAccessToken = jwtService.generateToken(userDetails);

        // Generate and save new opaque refresh token with incremented refreshCount
        String newRawRefreshToken = generateSecureRandomToken();
        HDRefreshTokenEntity newRefreshToken = new HDRefreshTokenEntity();
        newRefreshToken.setEmployee(employee);
        newRefreshToken.setToken(newRawRefreshToken);
        newRefreshToken.setRefreshCount(currentCount + 1);
        newRefreshToken.setExpiresAt(Instant.now().plusMillis(refreshTokenExpirationMs));
        newRefreshToken.setRevoked(false);
        refreshTokenRepository.save(newRefreshToken);

        boolean mustChangePassword = !Boolean.TRUE.equals(employee.getActivated());

        return LoginResponseDTO.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRawRefreshToken)
                .employeeId(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .email(employee.getEmail())
                .fullName(employee.getFirstName() + " " + employee.getLastName())
                .role(employee.getRole())
                .departmentId(employee.getDepartment() != null ? employee.getDepartment().getId() : null)
                .departmentName(employee.getDepartment() != null ? employee.getDepartment().getName() : null)
                .timezone(employee.getTimezone())
                .mustChangePassword(mustChangePassword)
                .activated(employee.getActivated())
                .build();
    }
}