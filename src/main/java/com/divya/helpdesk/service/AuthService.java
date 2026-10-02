package com.divya.helpdesk.service;

import com.divya.helpdesk.dto.auth.*;

public interface AuthService {

    LoginResponseDTO login(LoginRequestDTO request);

    String forgotPassword(ForgotPasswordRequestDTO request);

    VerifyOtpResponseDTO verifyOtp(VerifyOtpRequestDTO request);

    String resetPassword(ResetPasswordRequestDTO request);

    String changePassword(String email, ChangePasswordRequestDTO request);

    LoginResponseDTO refreshToken(RefreshTokenRequestDTO request);
}