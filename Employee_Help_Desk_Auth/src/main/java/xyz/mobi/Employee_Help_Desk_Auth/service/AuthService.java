package xyz.mobi.Employee_Help_Desk_Auth.service;

import xyz.mobi.Employee_Help_Desk_Auth.dto.auth.*;

public interface AuthService {

    LoginResponseDTO login(LoginRequestDTO request);

    TokenRefreshResponseDTO refreshToken(RefreshTokenRequestDTO request);

    void changePassword(ChangePasswordRequestDTO request);

    void requestForgotPasswordOtp(ForgotPasswordOtpRequestDTO request);

    VerifyOtpResponseDTO verifyOtp(VerifyOtpRequestDTO request);

    void resetPassword(ResetPasswordRequestDTO request);

    Long getCurrentEmployeeId();
}
