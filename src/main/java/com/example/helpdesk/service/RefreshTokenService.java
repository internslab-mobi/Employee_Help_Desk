package com.example.helpdesk.service;

import com.example.helpdesk.dto.response.RefreshTokenResponseDTO;
import com.example.helpdesk.entity.Employee;
import com.example.helpdesk.entity.RefreshToken;

public interface RefreshTokenService {
    RefreshToken createRefreshToken(Employee employee);
    RefreshTokenResponseDTO refreshAccessToken(String refreshToken);
}
