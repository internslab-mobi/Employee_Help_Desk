package xyz.mobi.employeehelpdesk.service;

import xyz.mobi.employeehelpdesk.dto.auth.*;
import xyz.mobi.employeehelpdesk.entity.enums.UserRole;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    TokenRefreshResponse refreshToken(RefreshTokenRequest request);

    void changePassword(ChangePasswordRequest request);

    void requestForgotPasswordOtp(ForgotPasswordOtpRequest request);

    void resetPasswordWithOtp(ResetPasswordWithOtpRequest request);

    Long getCurrentEmployeeId();

    UserRole getCurrentUserRole();

    String getCurrentUserEmail();

    String getCurrentUserTimezone();

    java.time.ZoneId getCurrentUserZoneId();
}
