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

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${security.jwt.secret:default-super-secret-jwt-signing-key-for-development-must-be-at-least-256-bits!}")
    private String jwtSecret;

    @Getter
    @Value("${security.jwt.expiration-ms:3600000}")
    private long expirationMs;

    public String generateToken(Long employeeId, UserRole role) {
        try {
            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + expirationMs);

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(employeeId.toString())
                    .claim("employeeId", employeeId)
                    .claim("role", role != null ? role.name() : UserRole.EMPLOYEE.name())
                    .issueTime(now)
                    .expirationTime(expiryDate)
                    .build();

            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.HS256),
                    claimsSet
            );

            JWSSigner signer = new MACSigner(getSigningKey());
            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (Exception ex) {
            log.error("Could not generate JWT token: {}", ex.getMessage(), ex);
            throw new RuntimeException("Error creating JWT token", ex);
        }
    }

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
            Object employeeIdClaim = signedJWT.getJWTClaimsSet().getClaim("employeeId");
            if (employeeIdClaim instanceof Number num) {
                return num.longValue();
            }
            String subject = signedJWT.getJWTClaimsSet().getSubject();
            return Long.parseLong(subject);
        } catch (Exception ex) {
            throw new RuntimeException("Could not extract employee ID from token", ex);
        }
    }

    public UserRole getRoleFromToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            String roleStr = signedJWT.getJWTClaimsSet().getStringClaim("role");
            return roleStr != null ? UserRole.valueOf(roleStr) : UserRole.EMPLOYEE;
        } catch (Exception ex) {
            return UserRole.EMPLOYEE;
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
