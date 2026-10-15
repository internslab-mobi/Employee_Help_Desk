package xyz.mobi.employeehelpdesk.service;

import xyz.mobi.employeehelpdesk.dto.auth.*;
import xyz.mobi.employeehelpdesk.entity.enums.UserRole;

public interface AuthService {

    LoginResponseDTO login(LoginRequestDTO request);

    TokenRefreshResponseDTO refreshToken(RefreshTokenRequestDTO request);

    void changePassword(ChangePasswordRequestDTO request);

    void requestForgotPasswordOtp(ForgotPasswordOtpRequestDTO request);

    VerifyOtpResponseDTO verifyOtp(VerifyOtpRequestDTO request);

    void resetPassword(ResetPasswordRequestDTO request);

    Long getCurrentEmployeeId();

    UserRole getCurrentUserRole();

    String getCurrentUserEmail();

    String getCurrentUserTimezone();

    java.time.ZoneId getCurrentUserZoneId();
}
