package com.divya.helpdesk.service.impl;

import com.divya.helpdesk.dto.auth.*;
import com.divya.helpdesk.entity.HDEmployee;
import com.divya.helpdesk.entity.HDRefreshToken;
import com.divya.helpdesk.exception.AccessDeniedException;
import com.divya.helpdesk.exception.AccountNotActivatedException;
import com.divya.helpdesk.exception.BadRequestException;
import com.divya.helpdesk.exception.ResourceNotFoundException;
import com.divya.helpdesk.repository.HDEmployeeRepository;
import com.divya.helpdesk.repository.HDRefreshTokenRepository;
import com.divya.helpdesk.security.JWTService;
import com.divya.helpdesk.service.AuthService;
import com.divya.helpdesk.service.EmailService;
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

    private final SecureRandom secureRandom = new SecureRandom();

    /*
     * Temporary in-memory storage.
     */
    private final Map<String, String> otpStore = new ConcurrentHashMap<>();
    private final Map<String, String> resetTokenStore = new ConcurrentHashMap<>();
    private final Map<String, ActivationToken> activationTokenStore = new ConcurrentHashMap<>();

    private String generateSecureRandomToken() {
        byte[] randomBytes = new byte[64];
        secureRandom.nextBytes(randomBytes);
        return HexFormat.of().formatHex(randomBytes);
    }

    // LOGIN
    @Transactional
    @Override
    public LoginResponse login(LoginRequest request) {
        if (request == null || request.getEmail() == null || request.getPassword() == null) {
            throw new BadRequestException("Email and password are required");
        }

        String email = request.getEmail().trim().toLowerCase();
        HDEmployee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!Boolean.TRUE.equals(employee.getActivated())) {
            throw new AccountNotActivatedException("Account is not activated. Please activate your account first.");
        }

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
        HDRefreshToken refreshToken = new HDRefreshToken();
        refreshToken.setEmployee(employee);
        refreshToken.setToken(rawRefreshToken);
        refreshToken.setRefreshCount(0);
        refreshToken.setExpiresAt(Instant.now().plusMillis(refreshTokenExpirationMs));
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .employeeId(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .email(employee.getEmail())
                .fullName(employee.getFirstName() + " " + employee.getLastName())
                .role(employee.getRole())
                .departmentId(employee.getDepartment() != null ? employee.getDepartment().getId() : null)
                .departmentName(employee.getDepartment() != null ? employee.getDepartment().getName() : null)
                .timezone(employee.getTimezone())
                .build();
    }

    // ACTIVATE ACCOUNT

    // ACTIVATE ACCOUNT
    @Override
    public AccountActivationResponse activateAccount(AccountActivationRequest request) {
        if (request == null || request.getEmail() == null || request.getTemporaryPassword() == null) {
            throw new BadRequestException("Email and temporary password are required");
        }

        String email = request.getEmail().trim().toLowerCase();
        HDEmployee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with email: " + request.getEmail()));

        if (Boolean.TRUE.equals(employee.getActivated())) {
            throw new BadRequestException("Account is already activated");
        }
        if (!Boolean.TRUE.equals(employee.getEnabled())) {
            throw new AccessDeniedException("Account is disabled");
        }

        // Verify temporary password
        if (!passwordEncoder.matches(request.getTemporaryPassword(), employee.getPassword())) {
            throw new BadRequestException("Invalid temporary password");
        }

        // Activate account
        employee.setActivated(true);
        employeeRepository.save(employee);

        // Generate short-lived token (15 minutes for user convenience)
        String token = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES);
        activationTokenStore.put(email, new ActivationToken(token, expiresAt));
        resetTokenStore.put(email, token);

        return AccountActivationResponse.builder()
                .message("Account activated. Please change your temporary password.")
                .activationToken(token)
                .build();
    }

    // FORGOT PASSWORD - SEND OTP
    @Override
    public String forgotPassword(ForgotPasswordRequest request) {
        if (request == null || request.getEmail() == null || request.getEmail().isBlank()) {
            throw new BadRequestException("Email is required");
        }

        String email = request.getEmail().trim().toLowerCase();
        HDEmployee employee = employeeRepository.findByEmail(email)
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
         * Generate six-digit OTP.
         */
        String otp = String.valueOf((int) (Math.random() * 900000) + 100000);
        otpStore.put(email, otp);

        emailService.sendOtpEmail(employee.getEmail(), otp);
        return "OTP sent successfully";
    }

    // VERIFY OTP
    @Override
    public VerifyOtpResponse verifyOtp(VerifyOtpRequest request) {
        if (request == null || request.getEmail() == null || request.getOtp() == null) {
            throw new BadRequestException("Email and OTP are required");
        }

        String email = request.getEmail().trim().toLowerCase();
        String storedOtp = otpStore.get(email);

        if (storedOtp == null) {
            throw new BadRequestException("OTP not found or expired");
        }

        if (!storedOtp.equals(request.getOtp().trim())) {
            throw new BadRequestException("Invalid OTP");
        }

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

        return VerifyOtpResponse.builder()
                .email(request.getEmail())
                .resetToken(resetToken)
                .message("OTP verified successfully")
                .build();
    }

    // RESET PASSWORD
    @Override
    public String resetPassword(ResetPasswordRequest request) {
        if (request == null || request.getEmail() == null || request.getResetToken() == null) {
            throw new BadRequestException("Email and reset token are required");
        }
        String email = request.getEmail().trim().toLowerCase();

        /*
         * Check whether OTP verification or Account Activation generated a valid reset token.
         */
        String storedToken = resetTokenStore.get(email);
        if (storedToken == null) {
            ActivationToken activationToken = activationTokenStore.get(email);
            if (activationToken != null && !activationToken.getExpiresAt().isBefore(Instant.now())) {
                storedToken = activationToken.getToken();
            }
        }

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
        HDEmployee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with email: " + request.getEmail()));
        /*
         * Update password.
         */
        employee.setPassword(passwordEncoder.encode(request.getNewPassword()));
        employeeRepository.save(employee);
        /*
         * Reset token becomes invalid after successful password reset.
         */
        resetTokenStore.remove(email);
        activationTokenStore.remove(email);
        return "Password reset successfully";
    }

    // CHANGE PASSWORD
    @Override
    public String changePassword(String email, ChangePasswordRequest request) {
        if (email == null || request == null) {
            throw new BadRequestException("Request and email are required");
        }
        HDEmployee employee = employeeRepository.findByEmail(email.trim().toLowerCase())
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
         * Update password.
         */
        employee.setPassword(passwordEncoder.encode(request.getNewPassword()));
        employeeRepository.save(employee);
        return "Password changed successfully";
    }

    // REFRESH TOKEN (Traditional Database-Backed Refresh Token with Rotation)
    @Transactional
    @Override
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        if (request == null || request.getToken() == null || request.getToken().isBlank()) {
            throw new BadRequestException("Token must not be empty");
        }

        String rawToken = request.getToken().trim();
        if (rawToken.startsWith("Bearer ")) {
            rawToken = rawToken.substring(7).trim();
        }

        // Database lookup with pessimistic write lock to prevent race conditions on concurrent refresh
        HDRefreshToken tokenRecord = refreshTokenRepository.findByTokenForUpdate(rawToken)
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

        HDEmployee employee = tokenRecord.getEmployee();
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
        HDRefreshToken newRefreshToken = new HDRefreshToken();
        newRefreshToken.setEmployee(employee);
        newRefreshToken.setToken(newRawRefreshToken);
        newRefreshToken.setRefreshCount(currentCount + 1);
        newRefreshToken.setExpiresAt(Instant.now().plusMillis(refreshTokenExpirationMs));
        newRefreshToken.setRevoked(false);
        refreshTokenRepository.save(newRefreshToken);

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRawRefreshToken)
                .tokenType("Bearer")
                .employeeId(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .email(employee.getEmail())
                .fullName(employee.getFirstName() + " " + employee.getLastName())
                .role(employee.getRole())
                .departmentId(employee.getDepartment() != null ? employee.getDepartment().getId() : null)
                .departmentName(employee.getDepartment() != null ? employee.getDepartment().getName() : null)
                .timezone(employee.getTimezone())
                .build();
    }
}