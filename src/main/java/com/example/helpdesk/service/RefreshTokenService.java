package com.example.helpdesk.service;

import com.example.helpdesk.dto.response.RefreshTokenResponse;
import com.example.helpdesk.entity.Employee;
import com.example.helpdesk.entity.RefreshToken;
import com.example.helpdesk.exception.AuthenticationException;
import com.example.helpdesk.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    @Value("${app.jwt.refresh.expiration:604800000}")
    private long refreshExpiration;

    private static final int MAX_REFRESH_COUNT = 3;

    public RefreshToken createRefreshToken(Employee employee) {
        refreshTokenRepository.deleteByEmployeeId(employee.getId());

        String token = jwtService.generateRefreshToken();
        Instant expiryDate = Instant.now().plus(Duration.ofMillis(refreshExpiration));

        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .employee(employee)
                .expiryDate(expiryDate)
                .refreshCount(0)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshTokenResponse refreshAccessToken(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new AuthenticationException("ERR_017", "Invalid refresh token"));

        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new AuthenticationException("ERR_017", "Refresh token has expired");
        }

        if (refreshToken.getRefreshCount() >= MAX_REFRESH_COUNT) {
            refreshTokenRepository.delete(refreshToken);
            throw new AuthenticationException("ERR_017", "Refresh limit exceeded. Please login again");
        }

        Employee employee = refreshToken.getEmployee();
        String newAccessToken = jwtService.generateToken(
                employee.getId(),
                employee.getEmail(),
                employee.getRole().name()
        );

        refreshToken.setRefreshCount(refreshToken.getRefreshCount() + 1);
        refreshTokenRepository.save(refreshToken);

        String employeeName = employee.getFirstName() + " " +
                (employee.getLastName() != null ? employee.getLastName() : "");

        return RefreshTokenResponse.builder()
                .token(newAccessToken)
                .tokenType("Bearer")
                .expiresIn(refreshExpiration / 1000)
                .refreshToken(refreshTokenValue)
                .employeeId(employee.getId())
                .employeeName(employeeName.trim())
                .email(employee.getEmail())
                .role(employee.getRole())
                .build();
    }

    public void deleteRefreshToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(refreshTokenRepository::delete);
    }
}
