package xyz.mobi.Employee_Help_Desk_Auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.mobi.Employee_Help_Desk_Auth.dto.auth.*;
import xyz.mobi.Employee_Help_Desk_Auth.exception.UnauthorizedException;
import xyz.mobi.Employee_Help_Desk_Auth.security.JwtTokenProvider;
import xyz.mobi.Employee_Help_Desk_Auth.service.AuthService;

import java.util.Map;

@RestController
@RequestMapping({"/auth/user", "/user", "/helpdesk/auth/user"})
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO request
    ) {
        log.info("Auth microservice login reached for email={}", request.email());
        LoginResponseDTO response = authService.login(request);

        ResponseCookie refreshCookie = createRefreshTokenCookie(
                response.refreshToken(),
                jwtTokenProvider.getRefreshExpirationMs() / 1000
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(response);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<TokenRefreshResponseDTO> refreshToken(
            @CookieValue(name = "refreshToken", required = false) String cookieRefreshToken,
            @RequestBody(required = false) RefreshTokenRequestDTO request
    ) {
        String tokenToUse = (cookieRefreshToken != null && !cookieRefreshToken.isBlank())
                ? cookieRefreshToken
                : (request != null ? request.refreshToken() : null);

        if (tokenToUse == null || tokenToUse.isBlank()) {
            throw new UnauthorizedException("Refresh token is required");
        }

        TokenRefreshResponseDTO response = authService.refreshToken(new RefreshTokenRequestDTO(tokenToUse));

        ResponseCookie refreshCookie = createRefreshTokenCookie(
                response.refreshToken(),
                jwtTokenProvider.getRefreshExpirationMs() / 1000
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        ResponseCookie clearCookie = createRefreshTokenCookie("", 0);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .body(Map.of("message", "Logged out successfully"));
    }

    private ResponseCookie createRefreshTokenCookie(String tokenValue, long maxAgeSeconds) {
        return ResponseCookie.from("refreshToken", tokenValue != null ? tokenValue : "")
                .httpOnly(true)
                .secure(false) // Set to false for HTTP on localhost; can be true behind HTTPS
                .path("/")
                .maxAge(maxAgeSeconds)
                .sameSite("Lax")
                .build();
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
