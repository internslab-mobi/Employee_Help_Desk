package xyz.mobi.Employee_Help_Desk_Auth.dto.auth;

public record TokenRefreshResponseDTO(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresIn
) {}
