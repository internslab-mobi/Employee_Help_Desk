package com.divya.helpdesk.service;

import org.springframework.security.core.userdetails.UserDetails;

public interface JWTService {

    String generateToken(UserDetails userDetails);

    String extractUsername(String token);

    Long extractEmployeeId(String token);

    boolean isTokenValid(String token, UserDetails userDetails);
}
