package xyz.mobi.employeehelpdesk.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.mobi.employeehelpdesk.dto.auth.*;
import xyz.mobi.employeehelpdesk.service.AuthService;

import java.util.Map;

@Slf4j
@Tag(name = "Authentication", description = "Authentication and Password Management APIs")
@RestController
@RequestMapping({"/user", "/helpdesk/auth/user", "/auth/user", "/api/user", "/api/auth", "/helpdesk/user"})
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "User login with email and password")
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO request
    ) {
        log.info("Processing login request for email={}", request.email());
        LoginResponseDTO response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Refresh JWT access token")
    @PostMapping("/refresh-token")
    public ResponseEntity<TokenRefreshResponseDTO> refreshToken(
            @Valid @RequestBody RefreshTokenRequestDTO request
    ) {
        log.info("Processing refresh token request");
        TokenRefreshResponseDTO response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Change password for authenticated user")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequestDTO request
    ) {
        authService.changePassword(request);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    @Operation(summary = "Request password reset OTP")
    @PostMapping("/forgot-password/request-otp")
    public ResponseEntity<Map<String, String>> requestForgotPasswordOtp(
            @Valid @RequestBody ForgotPasswordOtpRequestDTO request
    ) {
        authService.requestForgotPasswordOtp(request);
        return ResponseEntity.ok(Map.of("message", "OTP sent successfully"));
    }

    @Operation(summary = "Verify password reset OTP")
    @PostMapping("/forgot-password/verify-otp")
    public ResponseEntity<VerifyOtpResponseDTO> verifyOtp(
            @Valid @RequestBody VerifyOtpRequestDTO request
    ) {
        VerifyOtpResponseDTO response = authService.verifyOtp(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Reset password using verified reset token")
    @PostMapping("/forgot-password/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDTO request
    ) {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }
}
