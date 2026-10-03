package com.example.helpdesk.service;

import com.example.helpdesk.entity.Employee;

public interface OTPService {

    String generateOTP();

    String hashOTP(String otp);

    boolean verifyOTP(String otp, String otpHash);

    String createAndStoreOTP(Employee employee);

    boolean validateOTPForEmployee(Long employeeId, String otp);
}




