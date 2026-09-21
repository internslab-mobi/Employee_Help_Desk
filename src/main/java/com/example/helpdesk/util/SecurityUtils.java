package com.example.helpdesk.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

/**
 * Utility methods for accessing authenticated user details from the Spring SecurityContext.
 */
public class SecurityUtils {

    private SecurityUtils() {
        // Private constructor for utility class
    }

    /**
     * Retrieves the current authenticated Jwt token from SecurityContext.
     */
    public static Optional<Jwt> getCurrentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return Optional.of(jwt);
        }
        return Optional.empty();
    }

    /**
     * Obtains the current authenticated employee ID from the JWT subject ('sub').
     */
    public static Optional<Long> getCurrentEmployeeId() {
        return getCurrentJwt().map(jwt -> {
            try {
                return Long.parseLong(jwt.getSubject());
            } catch (NumberFormatException e) {
                return null;
            }
        });
    }

    /**
     * Obtains the current authenticated employee email from the JWT 'email' claim.
     */
    public static Optional<String> getCurrentEmployeeEmail() {
        return getCurrentJwt().map(jwt -> jwt.getClaimAsString("email"));
    }

    /**
     * Obtains the current authenticated employee role from the JWT 'role' claim.
     */
    public static Optional<String> getCurrentEmployeeRole() {
        return getCurrentJwt().map(jwt -> jwt.getClaimAsString("role"));
    }
}
