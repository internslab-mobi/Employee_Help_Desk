package xyz.mobi.employeehelpdesk.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import xyz.mobi.employeehelpdesk.entity.enums.UserRole;
import xyz.mobi.employeehelpdesk.exception.UnauthorizedException;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private final String secret = "test-jwt-secret-key-that-is-at-least-256-bits-long-for-hs256-signing!";

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", secret);
        ReflectionTestUtils.setField(jwtTokenProvider, "expirationMs", 900000L); // 15 mins
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshExpirationMs", 2700000L); // 45 mins
    }

    @Test
    @DisplayName("Should generate valid JWT token with claims and validate successfully")
    void shouldGenerateAndValidateToken() {
        Long employeeId = 42L;
        UserRole role = UserRole.EMPLOYEE;
        String timezone = "Asia/Kolkata";

        String token = jwtTokenProvider.generateToken(employeeId, role, timezone);

        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals(employeeId, jwtTokenProvider.getEmployeeIdFromToken(token));
        assertEquals(role, jwtTokenProvider.getRoleFromToken(token));
        assertEquals(timezone, jwtTokenProvider.getTimezoneFromToken(token));
    }

    @Test
    @DisplayName("Should validate tokens for different roles: ADMIN, AGENT, MANAGER")
    void shouldSupportAllRolesInToken() {
        for (UserRole role : UserRole.values()) {
            String token = jwtTokenProvider.generateToken(100L, role, "UTC");
            assertTrue(jwtTokenProvider.validateToken(token));
            assertEquals(role, jwtTokenProvider.getRoleFromToken(token));
        }
    }

    @Test
    @DisplayName("Should return false for invalid or tampered tokens")
    void shouldRejectInvalidToken() {
        assertFalse(jwtTokenProvider.validateToken("invalid.jwt.token"));
        assertFalse(jwtTokenProvider.validateToken(""));
        assertFalse(jwtTokenProvider.validateToken(null));
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when parsing invalid token for employee ID")
    void shouldThrowWhenExtractingFromInvalidToken() {
        assertThrows(UnauthorizedException.class, () -> jwtTokenProvider.getEmployeeIdFromToken("invalid-token"));
        assertThrows(UnauthorizedException.class, () -> jwtTokenProvider.getRoleFromToken("invalid-token"));
    }
}
