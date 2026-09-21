package xyz.mobi.employeehelpdesk.dto.employee;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import xyz.mobi.employeehelpdesk.entity.enums.EmploymentStatus;
import xyz.mobi.employeehelpdesk.entity.enums.UserRole;

import java.time.LocalDate;

@Builder
public record CreateEmployeeRequest(
        @NotBlank(message = "Employee code is required")
        @Size(max = 50, message = "Employee code cannot exceed 50 characters")
        String employeeCode,

        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name cannot exceed 100 characters")
        String firstName,

        @Size(max = 100, message = "Last name cannot exceed 100 characters")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 255, message = "Email cannot exceed 255 characters")
        String email,

        @Size(max = 30, message = "Phone number cannot exceed 30 characters")
        String phone,

        @Size(max = 100, message = "Designation cannot exceed 100 characters")
        String designation,

        Long departmentId,

        @NotNull(message = "Employment status is required")
        EmploymentStatus employmentStatus,

        @NotNull(message = "Role is required")
        UserRole role,

        @Size(min = 6, message = "Initial password must be at least 6 characters")
        String initialPassword,

        LocalDate dateOfJoining
) {
}
