package com.divya.helpdesk.dto.user;

import com.divya.helpdesk.enums.EmployeeRole;
import com.divya.helpdesk.enums.EmploymentStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateEmployeeRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    private String phone;
    private String designation;

    @NotNull(message = "Department is required")
    private Long departmentId;

    @NotNull(message = "Role is required")
    private EmployeeRole role;

    @NotNull(message = "Employment status is required")
    private EmploymentStatus employmentStatus;

    private LocalDate dateOfJoining;
    private String timezone;
}