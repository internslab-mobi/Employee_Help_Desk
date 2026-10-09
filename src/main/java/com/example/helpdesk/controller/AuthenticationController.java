package com.example.helpdesk.controller;

import com.example.helpdesk.dto.request.FirstLoginPasswordResetRequestDTO;
import com.example.helpdesk.dto.request.FirstLoginRequestDTO;
import com.example.helpdesk.dto.request.LoginRequestDTO;
import com.example.helpdesk.dto.request.RefreshTokenRequestDTO;
import com.example.helpdesk.dto.response.LoginResponseDTO;
import com.example.helpdesk.dto.response.RefreshTokenResponseDTO;
import com.example.helpdesk.service.AuthenticationService;
import com.example.helpdesk.service.RefreshTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication endpoints")
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    @Operation(summary = "Login", description = "🔐 Access: All roles — login is public and returns a JWT after successful authentication.")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.status(HttpStatus.OK).body(authenticationService.login(request));
    }

    @PostMapping("/first-login")
    @Operation(summary = "First Login with OTP", description = "🔐 Access: All roles — first login with temporary password and OTP for new accounts.")
    public ResponseEntity<LoginResponseDTO> firstLogin(@Valid @RequestBody FirstLoginRequestDTO request) {
        return ResponseEntity.status(HttpStatus.OK).body(authenticationService.firstLogin(request));
    }

    @PostMapping("/first-login/reset-password")
    @Operation(summary = "Reset Password After First Login", description = "🔐 Access: All roles — set permanent password after first login with temporary password.")
    public ResponseEntity<Void> resetFirstLoginPassword(@Valid @RequestBody FirstLoginPasswordResetRequestDTO request) {
        authenticationService.resetFirstLoginPassword(request);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh Access Token", description = "🔐 Access: All roles — refresh access token using refresh token. Maximum 3 refreshes per session.")
    public ResponseEntity<RefreshTokenResponseDTO> refreshToken(@Valid @RequestBody RefreshTokenRequestDTO request) {
        return ResponseEntity.status(HttpStatus.OK).body(refreshTokenService.refreshAccessToken(request.getRefreshToken()));
    }
}




