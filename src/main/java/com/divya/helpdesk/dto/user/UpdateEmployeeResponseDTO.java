package com.divya.helpdesk.dto.user;

import com.divya.helpdesk.enums.EmployeeRole;
import com.divya.helpdesk.enums.EmploymentStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEmployeeResponseDTO {

    private Long id;
    private String employeeCode;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String designation;
    private DepartmentDTO department;
    private EmployeeRole role;
    private EmploymentStatus employmentStatus;
    private LocalDate dateOfJoining;
    private String timezone;
    private OffsetDateTime createdAt;
    private OffsetDateTime  updatedAt;
}
