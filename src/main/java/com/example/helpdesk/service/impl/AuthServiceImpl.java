package com.example.helpdesk.service.impl;

import com.example.helpdesk.dto.request.LoginRequest;
import com.example.helpdesk.dto.response.LoginResponse;
import com.example.helpdesk.entity.Employee;
import com.example.helpdesk.exception.AuthenticationException;
import com.example.helpdesk.repository.EmployeeRepository;
import com.example.helpdesk.service.AuthService;
import com.example.helpdesk.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthServiceImpl implements AuthService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${app.jwt.expiration}")
    private long jwtExpiration;

    @Override
    public LoginResponse login(LoginRequest request) {
        Employee employee = employeeRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthenticationException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), employee.getPasswordHash())) {
            throw new AuthenticationException("Invalid email or password");
        }

        String token = jwtService.generateToken(
                employee.getId(),
                employee.getEmail(),
                employee.getRole().name()
        );

        String employeeName = employee.getFirstName() + " " +
                (employee.getLastName() != null ? employee.getLastName() : "");

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtExpiration / 1000)
                .employeeId(employee.getId())
                .employeeName(employeeName.trim())
                .email(employee.getEmail())
                .role(employee.getRole())
                .build();
    }
}
