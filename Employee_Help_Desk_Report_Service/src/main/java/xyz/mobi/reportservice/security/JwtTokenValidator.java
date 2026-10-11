package xyz.mobi.reportservice.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

@Slf4j
@Component
public class JwtTokenValidator {

    @Value("${security.jwt.secret:this-is-my-superkey-for-employee-helpdesk}")
    private String jwtSecret;

    public Optional<ReportUserPrincipal> validateAndParse(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        try {
            SignedJWT signedJWT = SignedJWT.parse(token);

            if (!JWSAlgorithm.HS256.equals(signedJWT.getHeader().getAlgorithm())) {
                log.warn("Rejected JWT with unexpected algorithm: {}", signedJWT.getHeader().getAlgorithm());
                return Optional.empty();
            }

            byte[] secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
            if (secretBytes.length < 32) {
                byte[] padded = new byte[32];
                System.arraycopy(secretBytes, 0, padded, 0, secretBytes.length);
                secretBytes = padded;
            }

            JWSVerifier verifier = new MACVerifier(secretBytes);
            if (!signedJWT.verify(verifier)) {
                log.warn("JWT signature verification failed");
                return Optional.empty();
            }

            Date expiration = signedJWT.getJWTClaimsSet().getExpirationTime();
            if (expiration != null && expiration.before(new Date())) {
                log.warn("JWT token is expired");
                return Optional.empty();
            }

            String subject = signedJWT.getJWTClaimsSet().getSubject();
            if (subject == null || subject.isBlank()) {
                log.warn("JWT token missing subject (employeeId)");
                return Optional.empty();
            }

            Long employeeId = Long.parseLong(subject);
            String role = signedJWT.getJWTClaimsSet().getStringClaim("role");
            String timezone = signedJWT.getJWTClaimsSet().getStringClaim("timezone");

            return Optional.of(new ReportUserPrincipal(employeeId, role, timezone, token));
        } catch (Exception e) {
            log.warn("Failed to validate JWT: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
