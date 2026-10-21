package com.example.helpdesk.service.impl;

import com.example.helpdesk.dto.request.FirstLoginPasswordResetRequestDTO;
import com.example.helpdesk.dto.request.FirstLoginRequestDTO;
import com.example.helpdesk.dto.request.LoginRequestDTO;
import com.example.helpdesk.dto.response.LoginResponseDTO;
import com.example.helpdesk.entity.Employee;
import com.example.helpdesk.entity.RefreshToken;
import com.example.helpdesk.exception.AuthenticationException;
import com.example.helpdesk.repository.EmployeeRepository;
import com.example.helpdesk.security.EmployeeUserDetails;
import com.example.helpdesk.service.AuthenticationService;
import com.example.helpdesk.service.JwtService;
import com.example.helpdesk.service.OTPService;
import com.example.helpdesk.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthenticationServiceImpl implements AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final OTPService otpService;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.jwt.expiration}")
    private long jwtExpiration;

    @Override
    public LoginResponseDTO login(LoginRequestDTO request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            throw new AuthenticationException("Invalid email or password");
        } catch (org.springframework.security.core.AuthenticationException e) {
            throw new AuthenticationException(e.getMessage());
        }

        EmployeeUserDetails userDetails = (EmployeeUserDetails) authentication.getPrincipal();
        Employee employee = userDetails.getEmployee();

        String token = jwtService.generateToken(
                employee.getId(),
                employee.getEmail(),
                employee.getRole().name()
        );

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(employee);

        String employeeName = employee.getFirstName() + " " +
                (employee.getLastName() != null ? employee.getLastName() : "");

        return LoginResponseDTO.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtExpiration / 1000)
                .refreshToken(refreshToken.getToken())
                .employeeId(employee.getId())
                .employeeName(employeeName.trim())
                .email(employee.getEmail())
                .role(employee.getRole())
                .build();
    }

    @Override
    public LoginResponseDTO firstLogin(FirstLoginRequestDTO request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            throw new AuthenticationException("Invalid email or password");
        } catch (org.springframework.security.core.AuthenticationException e) {
            throw new AuthenticationException(e.getMessage());
        }

        EmployeeUserDetails userDetails = (EmployeeUserDetails) authentication.getPrincipal();
        Employee employee = userDetails.getEmployee();

        if (!employee.getMustChangePassword()) {
            throw new AuthenticationException("ERR_023", "First login is not required for this account");
        }

        boolean otpValid = otpService.validateOTPForEmployee(employee.getId(), request.getOtp());
        if (!otpValid) {
            throw new AuthenticationException("ERR_021", "Invalid or expired OTP");
        }

        String token = jwtService.generateToken(
                employee.getId(),
                employee.getEmail(),
                employee.getRole().name()
        );

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(employee);

        String employeeName = employee.getFirstName() + " " +
                (employee.getLastName() != null ? employee.getLastName() : "");

        return LoginResponseDTO.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtExpiration / 1000)
                .refreshToken(refreshToken.getToken())
                .employeeId(employee.getId())
                .employeeName(employeeName.trim())
                .email(employee.getEmail())
                .role(employee.getRole())
                .build();
    }

    @Override
    public void resetFirstLoginPassword(FirstLoginPasswordResetRequestDTO request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new AuthenticationException("ERR_024", "Password and confirm password do not match");
        }

        Employee employee = employeeRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthenticationException("Employee not found with email: " + request.getEmail()));

        if (!employee.getMustChangePassword()) {
            throw new AuthenticationException("ERR_025", "First login password reset is not required for this account");
        }

        String newPasswordHash = passwordEncoder.encode(request.getPassword());
        employee.setPasswordHash(newPasswordHash);
        employee.setMustChangePassword(false);
        employeeRepository.save(employee);

        log.info("Password reset completed for employee {}", employee.getEmail());
    }
}




