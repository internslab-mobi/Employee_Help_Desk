package com.example.helpdesk.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;

    @Value("${app.jwt.expiration}")
    private long expiration;

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final int REFRESH_TOKEN_LENGTH = 128;

    public JwtService(JwtEncoder jwtEncoder, JwtDecoder jwtDecoder) {
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
    }


    public String generateToken(Long employeeId, String email, String role) {
        Instant now = Instant.now();
        Instant expiry = now.plus(expiration, ChronoUnit.MILLIS);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("helpdesk")
                .issuedAt(now)
                .expiresAt(expiry)
                .subject(employeeId.toString())
                .claim("email", email)
                .claim("role", role)
                .build();

        JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();

        return this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }

    public String generateRefreshToken() {
        byte[] randomBytes = new byte[REFRESH_TOKEN_LENGTH];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public String extractEmployeeId(String token) {
        Jwt jwt = jwtDecoder.decode(token);
        return jwt.getSubject();
    }

    public String extractEmail(String token) {
        Jwt jwt = jwtDecoder.decode(token);
        return jwt.getClaim("email");
    }

    public String extractRole(String token) {
        Jwt jwt = jwtDecoder.decode(token);
        return jwt.getClaim("role");
    }

    public boolean isTokenValid(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            return jwt.getExpiresAt() != null && jwt.getExpiresAt().isAfter(Instant.now());
        } catch (Exception e) {
            return false;
        }
    }
}




