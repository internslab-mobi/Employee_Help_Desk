package xyz.mobi.employeehelpdesk.dto.auth;

import xyz.mobi.employeehelpdesk.entity.enums.UserRole;

public record LoginResponse(
        String accessToken,
        String tokenType,
        Long expiresIn,
        Long employeeId,
        UserRole role
) {}
