package com.example.helpdesk.service.impl;

import com.example.helpdesk.entity.Employee;
import com.example.helpdesk.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    @Override
    public void sendAccountCreatedEmail(Employee employee, String temporaryPassword, String otp, int otpExpirationMinutes) {
        log.info("Sending account creation email to: {}", employee.getEmail());
        log.info("Temporary password: [REDACTED]");
        log.info("OTP: [REDACTED]");
        log.info("OTP expires in: {} minutes", otpExpirationMinutes);

    }
}
