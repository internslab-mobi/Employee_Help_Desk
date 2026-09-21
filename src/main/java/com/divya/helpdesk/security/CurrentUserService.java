package com.divya.helpdesk.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final HttpServletRequest httpRequest;
    private final JWTService jwtService;

    public Long getEmployeeId() {
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalStateException("Authorization token is missing");
        }
        String token = authHeader.substring(7);

        return jwtService.extractEmployeeId(token);
    }
}