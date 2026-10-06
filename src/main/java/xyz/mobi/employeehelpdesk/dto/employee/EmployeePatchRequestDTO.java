package xyz.mobi.employeehelpdesk.dto.employee;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.*;
import xyz.mobi.employeehelpdesk.entity.enums.EmploymentStatus;
import xyz.mobi.employeehelpdesk.entity.enums.UserRole;
import xyz.mobi.employeehelpdesk.validator.ValidTimezone;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeePatchRequestDTO {

    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email cannot exceed 255 characters")
    private String email;

    @Size(max = 100, message = "First name cannot exceed 100 characters")
    private String firstName;

    @Size(max = 100, message = "Last name cannot exceed 100 characters")
    private String lastName;

    @Size(max = 100, message = "Designation cannot exceed 100 characters")
    private String designation;

    @Size(max = 30, message = "Phone cannot exceed 30 characters")
    private String phone;

    private Long departmentId;

    private EmploymentStatus employmentStatus;

    private UserRole role;

    private LocalDate dateOfExit;

    @ValidTimezone
    private String timezone;

    private Long managerId;
}
