package com.example.helpdesk.dto.response;

import com.example.helpdesk.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String tokenType;
    private long expiresIn;
    private Long employeeId;
    private String employeeName;
    private String email;
    private Role role;
}
