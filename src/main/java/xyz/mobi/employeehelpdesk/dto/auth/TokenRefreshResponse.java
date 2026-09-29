package xyz.mobi.employeehelpdesk.dto.auth;

public record TokenRefreshResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresIn
) {}
