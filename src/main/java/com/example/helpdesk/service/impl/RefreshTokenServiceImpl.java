package com.example.helpdesk.service.impl;

import com.example.helpdesk.dto.response.RefreshTokenResponseDTO;
import com.example.helpdesk.entity.Employee;
import com.example.helpdesk.entity.RefreshToken;
import com.example.helpdesk.exception.AuthenticationException;
import com.example.helpdesk.repository.RefreshTokenRepository;
import com.example.helpdesk.service.JwtService;
import com.example.helpdesk.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    @Value("${app.jwt.refresh-expiration:604800000}")
    private long refreshExpiration;

    @Override
    @Transactional
    public RefreshToken createRefreshToken(Employee employee) {
        refreshTokenRepository.deleteByEmployeeId(employee.getId());

        String token = jwtService.generateRefreshToken();
        Instant expiryDate = Instant.now().plus(refreshExpiration, ChronoUnit.MILLIS);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .employee(employee)
                .expiryDate(expiryDate)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        refreshToken = refreshTokenRepository.save(refreshToken);
        log.info("Created refresh token for employee {}", employee.getEmail());
        return refreshToken;
    }

    @Override
    @Transactional
    public RefreshTokenResponseDTO refreshAccessToken(String refreshToken) {
        RefreshToken token = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new AuthenticationException("Invalid refresh token"));

        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new AuthenticationException("Refresh token expired");
        }

        Employee employee = token.getEmployee();

        String newAccessToken = jwtService.generateToken(
                employee.getId(),
                employee.getEmail(),
                employee.getRole().name()
        );

        String newRefreshToken = jwtService.generateRefreshToken();
        token.setToken(newRefreshToken);
        token.setExpiryDate(Instant.now().plus(refreshExpiration, ChronoUnit.MILLIS));
        token.setUpdatedAt(Instant.now());
        refreshTokenRepository.save(token);

        String employeeName = employee.getFirstName() + " " +
                (employee.getLastName() != null ? employee.getLastName() : "");

        return RefreshTokenResponseDTO.builder()
                .token(newAccessToken)
                .tokenType("Bearer")
                .expiresIn(refreshExpiration / 1000)
                .refreshToken(newRefreshToken)
                .employeeId(employee.getId())
                .employeeName(employeeName.trim())
                .email(employee.getEmail())
                .role(employee.getRole())
                .build();
    }
}
