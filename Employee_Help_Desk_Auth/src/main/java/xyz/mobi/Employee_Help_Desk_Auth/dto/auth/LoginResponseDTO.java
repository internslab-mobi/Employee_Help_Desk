package xyz.mobi.Employee_Help_Desk_Auth.dto.auth;

import xyz.mobi.Employee_Help_Desk_Auth.entity.enums.UserRole;

public record LoginResponseDTO(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresIn,
        Long employeeId,
        UserRole role
) {
    public LoginResponseDTO(String accessToken, String tokenType, Long expiresIn, Long employeeId, UserRole role) {
        this(accessToken, null, tokenType, expiresIn, employeeId, role);
    }
}
