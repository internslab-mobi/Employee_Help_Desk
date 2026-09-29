package com.divya.helpdesk.dto.user;

import com.divya.helpdesk.enums.EmployeeRole;
import com.divya.helpdesk.enums.EmploymentStatus;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeUpdateResponse {

    private Long id;
    private String employeeCode;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String designation;
    private DepartmentResponse department;
    private EmployeeRole role;
    private EmploymentStatus employmentStatus;
    private LocalDate dateOfJoining;
    private String timezone;
    private Instant createdAt;
    private Instant updatedAt;
}
