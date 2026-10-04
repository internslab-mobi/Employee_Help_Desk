package xyz.mobi.employeehelpdesk.dto.auth;

import xyz.mobi.employeehelpdesk.entity.enums.UserRole;

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
