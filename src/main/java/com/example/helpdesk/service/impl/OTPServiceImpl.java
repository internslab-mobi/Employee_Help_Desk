package com.example.helpdesk.service.impl;

import com.example.helpdesk.entity.Employee;
import com.example.helpdesk.entity.LoginOTP;
import com.example.helpdesk.exception.AuthenticationException;
import com.example.helpdesk.repository.LoginOTPRepository;
import com.example.helpdesk.service.OTPService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class OTPServiceImpl implements OTPService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OTPServiceImpl.class);

    private final LoginOTPRepository loginOTPRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.otp-expiration-minutes:10}")
    private int otpExpirationMinutes;

    private static final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generateOTP() {
        int otp = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(otp);
    }

    @Override
    public String hashOTP(String otp) {
        return passwordEncoder.encode(otp);
    }

    @Override
    public boolean verifyOTP(String otp, String otpHash) {
        return passwordEncoder.matches(otp, otpHash);
    }

    @Override
    @Transactional
    public String createAndStoreOTP(Employee employee) {
        String otp = generateOTP();
        String otpHash = hashOTP(otp);
        Instant expiresAt = Instant.now().plus(java.time.Duration.ofMinutes(otpExpirationMinutes));

        LoginOTP loginOTP = LoginOTP.builder()
                .employee(employee)
                .otpHash(otpHash)
                .expiresAt(expiresAt)
                .usedAt(null)
                .attemptCount(0)
                .build();

        loginOTPRepository.save(loginOTP);
        log.info("Created OTP for employee {} expiring at {}", employee.getEmail(), expiresAt);
        return otp;
    }

    @Override
    @Transactional
    public boolean validateOTPForEmployee(Long employeeId, String otp) {
        Instant now = Instant.now();
        LoginOTP loginOTP = loginOTPRepository
                .findByEmployeeIdAndUsedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(employeeId, now)
                .orElse(null);

        if (loginOTP == null) {
            log.warn("No valid OTP found for employee {}", employeeId);
            return false;
        }

        if (loginOTP.getExpiresAt().isBefore(now)) {
            log.warn("OTP expired for employee {}", employeeId);
            return false;
        }

        if (loginOTP.getUsedAt() != null) {
            log.warn("OTP already used for employee {}", employeeId);
            return false;
        }

        boolean isValid = verifyOTP(otp, loginOTP.getOtpHash());
        if (isValid) {
            loginOTP.setUsedAt(now);
            loginOTPRepository.save(loginOTP);
            log.info("OTP validated and marked as used for employee {}", employeeId);
        } else {
            loginOTP.setAttemptCount(loginOTP.getAttemptCount() + 1);
            loginOTPRepository.save(loginOTP);
            log.warn("Invalid OTP attempt {} for employee {}", loginOTP.getAttemptCount(), employeeId);
        }

        return isValid;
    }
}




