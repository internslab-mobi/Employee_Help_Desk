package com.divya.helpdesk.dto.response;

import com.divya.helpdesk.enums.HDEmploymentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeResponse {
    private Long id;
    private String employeeCode;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String phone;
    private String designation;
    private Long departmentId;
    private String departmentName;
    private HDEmploymentStatus employmentStatus;
    private LocalDate dateOfJoining;
    private LocalDate dateOfExit;
    private boolean hasProfileImage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
