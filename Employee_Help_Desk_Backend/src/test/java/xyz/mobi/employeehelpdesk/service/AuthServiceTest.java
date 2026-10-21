package xyz.mobi.employeehelpdesk.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import xyz.mobi.employeehelpdesk.dto.auth.*;
import xyz.mobi.employeehelpdesk.entity.Employee;
import xyz.mobi.employeehelpdesk.entity.PasswordResetOtp;
import xyz.mobi.employeehelpdesk.entity.RefreshToken;
import xyz.mobi.employeehelpdesk.entity.enums.EmploymentStatus;
import xyz.mobi.employeehelpdesk.entity.enums.UserRole;
import xyz.mobi.employeehelpdesk.exception.BadRequestException;
import xyz.mobi.employeehelpdesk.exception.UnauthorizedException;
import xyz.mobi.employeehelpdesk.repository.EmployeeRepository;
import xyz.mobi.employeehelpdesk.repository.PasswordResetOtpRepository;
import xyz.mobi.employeehelpdesk.repository.RefreshTokenRepository;
import xyz.mobi.employeehelpdesk.security.JwtTokenProvider;
import xyz.mobi.employeehelpdesk.service.helperservice.EmailService;
import xyz.mobi.employeehelpdesk.service.impl.AuthServiceImpl;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordResetOtpRepository passwordResetOtpRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private EmailService emailService;

    private AuthServiceImpl authService;

    private Employee testEmployee;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                employeeRepository,
                refreshTokenRepository,
                passwordResetOtpRepository,
                passwordEncoder,
                jwtTokenProvider,
                emailService
        );

        testEmployee = new Employee();
        testEmployee.setId(10L);
        testEmployee.setEmail("john.doe@company.com");
        testEmployee.setFirstName("John");
        testEmployee.setLastName("Doe");
        testEmployee.setPasswordHash("$2a$12$hashedPassword");
        testEmployee.setRole(UserRole.EMPLOYEE);
        testEmployee.setEmploymentStatus(EmploymentStatus.ACTIVE);
        testEmployee.setTimezone("Asia/Kolkata");
    }

    @Test
    @DisplayName("Login succeeds with valid credentials for active employee")
    void shouldLoginSuccessfully() {
        LoginRequestDTO request = new LoginRequestDTO("john.doe@company.com", "Password@123");

        when(employeeRepository.findByEmail("john.doe@company.com")).thenReturn(Optional.of(testEmployee));
        when(passwordEncoder.matches("Password@123", "$2a$12$hashedPassword")).thenReturn(true);
        when(jwtTokenProvider.generateToken(10L, UserRole.EMPLOYEE, "Asia/Kolkata")).thenReturn("mock.jwt.token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(900000L);
        when(jwtTokenProvider.getRefreshExpirationMs()).thenReturn(2700000L);

        LoginResponseDTO response = authService.login(request);

        assertNotNull(response);
        assertEquals("mock.jwt.token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(10L, response.employeeId());
        assertEquals(UserRole.EMPLOYEE, response.role());
        assertNotNull(response.refreshToken());

        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Login fails with invalid password")
    void shouldRejectInvalidPassword() {
        LoginRequestDTO request = new LoginRequestDTO("john.doe@company.com", "WrongPassword");

        when(employeeRepository.findByEmail("john.doe@company.com")).thenReturn(Optional.of(testEmployee));
        when(passwordEncoder.matches("WrongPassword", "$2a$12$hashedPassword")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Login fails with inactive employee")
    void shouldRejectInactiveEmployee() {
        testEmployee.setEmploymentStatus(EmploymentStatus.INACTIVE);
        LoginRequestDTO request = new LoginRequestDTO("john.doe@company.com", "Password@123");

        when(employeeRepository.findByEmail("john.doe@company.com")).thenReturn(Optional.of(testEmployee));
        when(passwordEncoder.matches("Password@123", "$2a$12$hashedPassword")).thenReturn(true);

        assertThrows(UnauthorizedException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Token refresh succeeds with valid refresh token")
    void shouldRefreshTokenSuccessfully() {
        RefreshToken refreshToken = RefreshToken.builder()
                .token("valid-uuid-refresh-token")
                .employee(testEmployee)
                .usageCount(0)
                .maxUses(3)
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .revoked(false)
                .build();

        when(refreshTokenRepository.incrementUsageIfValid(eq("valid-uuid-refresh-token"), any(Instant.class))).thenReturn(1);
        when(refreshTokenRepository.findByToken("valid-uuid-refresh-token")).thenReturn(Optional.of(refreshToken));
        when(jwtTokenProvider.generateToken(10L, UserRole.EMPLOYEE, "Asia/Kolkata")).thenReturn("new.mock.jwt.token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(900000L);

        TokenRefreshResponseDTO response = authService.refreshToken(new RefreshTokenRequestDTO("valid-uuid-refresh-token"));

        assertNotNull(response);
        assertEquals("new.mock.jwt.token", response.accessToken());
        assertEquals("valid-uuid-refresh-token", response.refreshToken());
    }

    @Test
    @DisplayName("Verify OTP succeeds with matching OTP")
    void shouldVerifyOtpSuccessfully() {
        PasswordResetOtp otpRecord = PasswordResetOtp.builder()
                .email("john.doe@company.com")
                .otpHash("$2a$12$otpHash")
                .otpExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .otpAttemptCount(0)
                .otpVerified(false)
                .build();

        when(employeeRepository.findByEmail("john.doe@company.com")).thenReturn(Optional.of(testEmployee));
        when(passwordResetOtpRepository.findTopByEmailOrderByCreatedAtDesc("john.doe@company.com")).thenReturn(Optional.of(otpRecord));
        when(passwordEncoder.matches("123456", "$2a$12$otpHash")).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$resetTokenHash");

        VerifyOtpResponseDTO response = authService.verifyOtp(new VerifyOtpRequestDTO("john.doe@company.com", "123456"));

        assertNotNull(response);
        assertNotNull(response.resetToken());
        assertTrue(otpRecord.isOtpVerified());
        verify(passwordResetOtpRepository).save(otpRecord);
    }

    @Test
    @DisplayName("Reset password succeeds with valid reset token")
    void shouldResetPasswordSuccessfully() {
        PasswordResetOtp otpRecord = PasswordResetOtp.builder()
                .email("john.doe@company.com")
                .otpHash("$2a$12$otpHash")
                .resetTokenHash("$2a$12$resetTokenHash")
                .resetTokenExpiresAt(Instant.now().plus(15, ChronoUnit.MINUTES))
                .resetTokenUsed(false)
                .otpVerified(true)
                .build();

        when(employeeRepository.findByEmail("john.doe@company.com")).thenReturn(Optional.of(testEmployee));
        when(passwordResetOtpRepository.findTopByEmailOrderByCreatedAtDesc("john.doe@company.com")).thenReturn(Optional.of(otpRecord));
        when(passwordEncoder.matches("my-reset-token", "$2a$12$resetTokenHash")).thenReturn(true);
        when(passwordEncoder.matches("NewPassword@123", "$2a$12$hashedPassword")).thenReturn(false);
        when(passwordEncoder.encode("NewPassword@123")).thenReturn("$2a$12$newHashedPassword");

        ResetPasswordRequestDTO request = new ResetPasswordRequestDTO(
                "john.doe@company.com",
                "my-reset-token",
                "NewPassword@123",
                "NewPassword@123"
        );

        authService.resetPassword(request);

        assertEquals("$2a$12$newHashedPassword", testEmployee.getPasswordHash());
        assertTrue(otpRecord.isResetTokenUsed());
        verify(employeeRepository).save(testEmployee);
        verify(refreshTokenRepository).deleteByEmployeeId(10L);
    }
}
