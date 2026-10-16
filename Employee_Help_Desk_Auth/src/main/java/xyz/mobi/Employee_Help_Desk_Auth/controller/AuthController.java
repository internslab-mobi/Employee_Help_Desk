package xyz.mobi.Employee_Help_Desk_Auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.mobi.Employee_Help_Desk_Auth.dto.auth.*;
import xyz.mobi.Employee_Help_Desk_Auth.service.AuthService;

import java.util.Map;

@RestController
@RequestMapping({"/helpdesk/auth/user", "/user", "/auth/user"})
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO request
    ) {
        log.info("Auth microservice login reached for email={}", request.email());
        LoginResponseDTO response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<TokenRefreshResponseDTO> refreshToken(
            @Valid @RequestBody RefreshTokenRequestDTO request
    ) {
        TokenRefreshResponseDTO response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequestDTO request
    ) {
        authService.changePassword(request);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    @PostMapping("/forgot-password/request-otp")
    public ResponseEntity<Map<String, String>> requestForgotPasswordOtp(
            @Valid @RequestBody ForgotPasswordOtpRequestDTO request
    ) {
        authService.requestForgotPasswordOtp(request);
        return ResponseEntity.ok(Map.of("message", "OTP sent successfully"));
    }

    @PostMapping("/forgot-password/verify-otp")
    public ResponseEntity<VerifyOtpResponseDTO> verifyOtp(
            @Valid @RequestBody VerifyOtpRequestDTO request
    ) {
        VerifyOtpResponseDTO response = authService.verifyOtp(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDTO request
    ) {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }
}
