package com.divya.helpdesk.dto.user;

import com.divya.helpdesk.enums.EmployeeRole;
import com.divya.helpdesk.enums.EmploymentStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEmployeeRequestDTO {

    @NotBlank(message = "Firstname is required!")
    private String firstName;

    @NotBlank(message = "Lastname is required!")
    private String lastName;

    @NotBlank(message = "Phone number is required!")
    private String phone;

    @NotBlank(message = "Designation is required!")
    private String designation;

    @NotBlank(message = "Department id is required!")
    private Long departmentId;

    @NotBlank(message = "Role is required!")
    private EmployeeRole role;

    @NotBlank(message = "Employee status is required!")
    private EmploymentStatus employmentStatus;

    @NotBlank(message = "Date of joining is required!")
    private LocalDate dateOfJoining;

    @NotBlank(message = "You missed out the enabled field!")
    private Boolean enabled;

    private String timezone;
}