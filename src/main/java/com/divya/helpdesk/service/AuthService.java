package com.divya.helpdesk.service;

import com.divya.helpdesk.dto.auth.*;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    AccountActivationResponse activateAccount(AccountActivationRequest request);

    String forgotPassword(ForgotPasswordRequest request);

    VerifyOtpResponse verifyOtp(VerifyOtpRequest request);

    String resetPassword(ResetPasswordRequest request);

    String changePassword(String email, ChangePasswordRequest request);

    LoginResponse refreshToken(RefreshTokenRequest request);
}