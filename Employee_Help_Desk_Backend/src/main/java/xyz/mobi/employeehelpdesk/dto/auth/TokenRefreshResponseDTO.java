package xyz.mobi.employeehelpdesk.dto.auth;

public record TokenRefreshResponseDTO(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresIn
) {}
