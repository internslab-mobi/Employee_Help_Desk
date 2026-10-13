package xyz.mobi.employeehelpdesk.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import xyz.mobi.employeehelpdesk.entity.enums.UserRole;
import xyz.mobi.employeehelpdesk.exception.UnauthorizedException;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${security.jwt.secret:default-super-secret-jwt-signing-key-for-development-must-be-at-least-256-bits!}")
    private String jwtSecret;

    @Getter
    @Value("${security.jwt.expiration-ms:900000}")
    private long expirationMs;

    @Getter
    @Value("${security.jwt.refresh-expiration-ms:2700000}")
    private long refreshExpirationMs;

    public boolean validateToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWSVerifier verifier = new MACVerifier(getSigningKey());

            if (!signedJWT.verify(verifier)) {
                log.warn("JWT token signature verification failed");
                return false;
            }

            Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
            if (expirationTime != null && expirationTime.before(new Date())) {
                log.warn("JWT token is expired");
                return false;
            }

            return true;
        } catch (Exception ex) {
            log.warn("Invalid JWT token: {}", ex.getMessage());
            return false;
        }
    }

    public Long getEmployeeIdFromToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            String subject = signedJWT.getJWTClaimsSet().getSubject();
            return Long.parseLong(subject);
        } catch (Exception ex) {
            log.warn("Could not extract employee ID from token: {}", ex.getMessage());
            throw new UnauthorizedException("Invalid token");
        }
    }

    public UserRole getRoleFromToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            String roleStr = signedJWT.getJWTClaimsSet().getStringClaim("role");
            if (roleStr == null || roleStr.isBlank()) {
                throw new UnauthorizedException("Token missing role claim");
            }
            return UserRole.valueOf(roleStr.trim());
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid role claim in token: {}", ex.getMessage());
            throw new UnauthorizedException("Invalid role claim in token");
        } catch (UnauthorizedException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Could not extract role from token: {}", ex.getMessage());
            throw new UnauthorizedException("Invalid token");
        }
    }

    public String getTimezoneFromToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return signedJWT.getJWTClaimsSet().getStringClaim("timezone");
        } catch (Exception ex) {
            log.warn("Could not extract timezone from token: {}", ex.getMessage());
            return null;
        }
    }

    private byte[] getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            return padded;
        }
        return keyBytes;
    }
}
