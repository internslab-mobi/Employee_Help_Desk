package xyz.mobi.employeehelpdesk.dto.department;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record CreateDepartmentRequest(
        @NotBlank(message = "Department code is required")
        @Size(max = 50, message = "Department code cannot exceed 50 characters")
        String code,

        @NotBlank(message = "Department name is required")
        @Size(max = 150, message = "Department name cannot exceed 150 characters")
        String name,

        String description
) {
}
