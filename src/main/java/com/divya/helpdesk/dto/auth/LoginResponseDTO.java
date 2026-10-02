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
public class LoginResponseDTO {

    private String accessToken;
    private String refreshToken;
    private Long employeeId;
    private String employeeCode;
    private String email;
    private String fullName;
    private EmployeeRole role;
    private Long departmentId;
    private String departmentName;
    private String timezone;
    private Boolean mustChangePassword;
    private Boolean activated;
}