package xyz.mobi.employeehelpdesk.dto.employee;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import xyz.mobi.employeehelpdesk.entity.enums.UserRole;
import xyz.mobi.employeehelpdesk.validator.ValidTimezone;

import java.time.LocalDate;

@Builder
public record CreateEmployeeRequestDTO(
        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name cannot exceed 100 characters")
        String firstName,

        @Size(max = 100, message = "Last name cannot exceed 100 characters")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 255, message = "Email cannot exceed 255 characters")
        String email,

        @Size(max = 30, message = "Phone cannot exceed 30 characters")
        String phone,

        @Size(max = 100, message = "Designation cannot exceed 100 characters")
        String designation,

        Long departmentId,

        Long managerId,

        UserRole role,

        LocalDate dateOfJoining,

        @ValidTimezone
        String timezone
) {
}
