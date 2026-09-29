package com.divya.helpdesk.dto.user;

import com.divya.helpdesk.enums.EmployeeRole;
import com.divya.helpdesk.enums.EmploymentStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class EmployeePatchRequest {

    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String designation;
    private Long departmentId;
    private EmployeeRole role;
    private EmploymentStatus employmentStatus;
    private LocalDate dateOfJoining;
    private LocalDate dateOfExit;
    private Boolean enabled;
    private Boolean activated;
    private String timezone;
}
