package com.example.helpdesk.service;

import com.example.helpdesk.dto.request.FirstLoginPasswordResetRequestDTO;
import com.example.helpdesk.dto.request.FirstLoginRequestDTO;
import com.example.helpdesk.dto.request.LoginRequestDTO;
import com.example.helpdesk.dto.response.LoginResponseDTO;

public interface AuthenticationService {
    LoginResponseDTO login(LoginRequestDTO request);
    LoginResponseDTO firstLogin(FirstLoginRequestDTO request);
    void resetFirstLoginPassword(FirstLoginPasswordResetRequestDTO request);
}
