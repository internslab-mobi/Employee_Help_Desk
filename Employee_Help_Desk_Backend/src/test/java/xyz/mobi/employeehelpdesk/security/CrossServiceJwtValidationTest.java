package xyz.mobi.employeehelpdesk.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import xyz.mobi.employeehelpdesk.entity.enums.UserRole;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class CrossServiceJwtValidationTest {

    private JwtTokenProvider backendTokenProvider;
    private final String sharedSecret = "this-is-my-superkey-for-employee-helpdesk";

    @BeforeEach
    void setUp() {
        backendTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(backendTokenProvider, "jwtSecret", sharedSecret);
        ReflectionTestUtils.setField(backendTokenProvider, "expirationMs", 900000L);
    }

    private String createToken(Long employeeId, UserRole role, String timezone, long expirationOffsetMs) throws Exception {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationOffsetMs);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(employeeId.toString())
                .claim("role", role.name())
                .claim("timezone", timezone)
                .issueTime(now)
                .expirationTime(expiry)
                .build();

        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
        byte[] keyBytes = sharedSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        JWSSigner signer = new MACSigner(keyBytes);
        signedJWT.sign(signer);
        return signedJWT.serialize();
    }

    @Test
    void testValidateTokenFromAuthService_Success() throws Exception {
        Long empId = 205L;
        UserRole role = UserRole.EMPLOYEE;
        String timezone = "UTC";

        String token = createToken(empId, role, timezone, 900000L);
        assertNotNull(token);

        assertTrue(backendTokenProvider.validateToken(token));
        assertEquals(empId, backendTokenProvider.getEmployeeIdFromToken(token));
        assertEquals(role, backendTokenProvider.getRoleFromToken(token));
        assertEquals(timezone, backendTokenProvider.getTimezoneFromToken(token));
    }

    @Test
    void testValidateToken_InvalidSignature_Rejected() throws Exception {
        String token = createToken(205L, UserRole.MANAGER, "UTC", 900000L);
        String tampered = token.substring(0, token.length() - 6) + "xyz123";

        assertFalse(backendTokenProvider.validateToken(tampered));
    }

    @Test
    void testValidateToken_Expired_Rejected() throws Exception {
        String expiredToken = createToken(205L, UserRole.ADMIN, "UTC", -5000L);
        assertFalse(backendTokenProvider.validateToken(expiredToken));
    }

    @Test
    void testValidateToken_Malformed_Rejected() {
        assertFalse(backendTokenProvider.validateToken("invalid.token.structure"));
    }
}
