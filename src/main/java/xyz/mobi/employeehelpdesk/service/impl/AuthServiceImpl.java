package xyz.mobi.employeehelpdesk.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.mobi.employeehelpdesk.dto.auth.LoginRequest;
import xyz.mobi.employeehelpdesk.dto.auth.LoginResponse;
import xyz.mobi.employeehelpdesk.entity.Employee;
import xyz.mobi.employeehelpdesk.entity.enums.EmploymentStatus;
import xyz.mobi.employeehelpdesk.entity.enums.UserRole;
import xyz.mobi.employeehelpdesk.exception.UnauthorizedException;
import xyz.mobi.employeehelpdesk.repository.EmployeeRepository;
import xyz.mobi.employeehelpdesk.security.JwtTokenProvider;
import xyz.mobi.employeehelpdesk.service.AuthService;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        if (request == null || request.email() == null || request.password() == null) {
            throw new UnauthorizedException("Invalid email or password");
        }

        Employee employee = employeeRepository.findByEmail(request.email().trim())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (employee.getPasswordHash() == null || !passwordEncoder.matches(request.password(), employee.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        if (!Boolean.TRUE.equals(employee.getEnabled())) {
            throw new UnauthorizedException("Account is disabled");
        }

        if (employee.getEmploymentStatus() != EmploymentStatus.ACTIVE) {
            throw new UnauthorizedException("Employee account is not active");
        }

        UserRole role = employee.getRole() != null ? employee.getRole() : UserRole.EMPLOYEE;
        String token = jwtTokenProvider.generateToken(employee.getId(), role);
        long expiresIn = jwtTokenProvider.getExpirationMs();

        return new LoginResponse(
                token,
                "Bearer",
                expiresIn,
                employee.getId(),
                role
        );
    }
}
