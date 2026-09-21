package com.example.helpdesk.service;

import com.example.helpdesk.dto.request.LoginRequest;
import com.example.helpdesk.dto.response.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);
}
