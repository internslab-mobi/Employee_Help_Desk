package xyz.mobi.employeehelpdesk.dto.department;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record UpdateDepartmentRequest(
        @NotBlank(message = "Department name is required")
        @Size(max = 150, message = "Department name cannot exceed 150 characters")
        String name,

        String description,

        @NotNull(message = "Active status is required")
        Boolean isActive
) {
}
