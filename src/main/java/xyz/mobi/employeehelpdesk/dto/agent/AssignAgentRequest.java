package xyz.mobi.employeehelpdesk.dto.agent;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record AssignAgentRequest(
        @NotNull(message = "Employee ID is required")
        Long employeeId
) {
}
