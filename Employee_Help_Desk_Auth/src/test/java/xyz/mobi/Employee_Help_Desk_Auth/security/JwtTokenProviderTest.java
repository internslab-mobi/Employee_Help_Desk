package xyz.mobi.Employee_Help_Desk_Auth.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import xyz.mobi.Employee_Help_Desk_Auth.entity.enums.UserRole;
import xyz.mobi.Employee_Help_Desk_Auth.exception.UnauthorizedException;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private final String secret = "default-super-secret-jwt-signing-key-for-development-must-be-at-least-256-bits!";

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(tokenProvider, "jwtSecret", secret);
        ReflectionTestUtils.setField(tokenProvider, "expirationMs", 900000L);
        ReflectionTestUtils.setField(tokenProvider, "refreshExpirationMs", 2700000L);
    }

    @Test
    void testGenerateAndValidateToken_Success() {
        Long empId = 101L;
        UserRole role = UserRole.EMPLOYEE;
        String timezone = "Asia/Kolkata";

        String token = tokenProvider.generateToken(empId, role, timezone);
        assertNotNull(token);
        assertTrue(tokenProvider.validateToken(token));

        assertEquals(empId, tokenProvider.getEmployeeIdFromToken(token));
        assertEquals(role, tokenProvider.getRoleFromToken(token));
        assertEquals(timezone, tokenProvider.getTimezoneFromToken(token));
    }

    @Test
    void testValidateToken_InvalidSignature() {
        Long empId = 101L;
        String token = tokenProvider.generateToken(empId, UserRole.ADMIN, "UTC");

        // Tamper token
        String tamperedToken = token.substring(0, token.length() - 5) + "abcde";
        assertFalse(tokenProvider.validateToken(tamperedToken));
    }

    @Test
    void testValidateToken_ExpiredToken() {
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(shortLivedProvider, "jwtSecret", secret);
        ReflectionTestUtils.setField(shortLivedProvider, "expirationMs", -1000L); // already expired

        String expiredToken = shortLivedProvider.generateToken(102L, UserRole.MANAGER, "UTC");
        assertNotNull(expiredToken);
        assertFalse(tokenProvider.validateToken(expiredToken));
    }

    @Test
    void testValidateToken_MalformedToken() {
        assertFalse(tokenProvider.validateToken("not-a-valid-jwt-token"));
    }
}
