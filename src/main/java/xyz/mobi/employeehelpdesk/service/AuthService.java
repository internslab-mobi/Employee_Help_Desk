package xyz.mobi.employeehelpdesk.service;

import xyz.mobi.employeehelpdesk.dto.auth.LoginRequest;
import xyz.mobi.employeehelpdesk.dto.auth.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);
}
