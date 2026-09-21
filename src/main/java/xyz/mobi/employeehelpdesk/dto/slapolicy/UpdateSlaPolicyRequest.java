package xyz.mobi.employeehelpdesk.dto.slapolicy;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

@Builder
public record UpdateSlaPolicyRequest(
        @NotNull(message = "Duration in minutes is required")
        @Positive(message = "Duration must be greater than 0")
        Integer durationMinutes,

        @Positive(message = "Warning duration must be greater than 0")
        Integer warningMinutes,

        @NotNull(message = "isActive flag is required")
        Boolean isActive
) {
}
