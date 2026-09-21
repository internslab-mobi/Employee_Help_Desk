package xyz.mobi.employeehelpdesk.dto.manager;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record AssignManagerRequest(
        @NotNull(message = "Employee ID is required")
        Long employeeId,

        Boolean isPrimary
) {
}
