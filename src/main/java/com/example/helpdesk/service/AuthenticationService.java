package com.example.helpdesk.service;

import com.example.helpdesk.dto.request.FirstLoginPasswordResetRequest;
import com.example.helpdesk.dto.request.FirstLoginRequest;
import com.example.helpdesk.dto.request.LoginRequest;
import com.example.helpdesk.dto.response.LoginResponse;

public interface AuthenticationService {

    LoginResponse login(LoginRequest request);

    LoginResponse firstLogin(FirstLoginRequest request);

    void resetFirstLoginPassword(FirstLoginPasswordResetRequest request);
}
