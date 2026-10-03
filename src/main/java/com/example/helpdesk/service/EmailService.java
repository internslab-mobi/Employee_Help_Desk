package com.example.helpdesk.service;

import com.example.helpdesk.entity.Employee;

public interface EmailService {

    void sendAccountCreatedEmail(Employee employee, String temporaryPassword, String otp, int otpExpirationMinutes);
}
