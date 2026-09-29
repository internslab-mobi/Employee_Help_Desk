package com.divya.helpdesk.dto.auth;

import com.divya.helpdesk.enums.EmployeeRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String accessToken;
    private String refreshToken;

    @Builder.Default
    private String tokenType = "Bearer";

    private Long employeeId;
    private String employeeCode;
    private String email;
    private String fullName;
    private EmployeeRole role;
    private Long departmentId;
    private String departmentName;
    private String timezone;
}