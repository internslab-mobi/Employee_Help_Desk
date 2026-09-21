package xyz.mobi.employeehelpdesk.dto.slapolicy;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

@Builder
public record CreateSlaPolicyRequest(
        @NotNull(message = "Department ID is required")
        Long departmentId,

        @NotNull(message = "SubCategory ID is required")
        Long subCategoryId,

        @NotNull(message = "Duration in minutes is required")
        @Positive(message = "Duration must be greater than 0")
        Integer durationMinutes,

        @Positive(message = "Warning duration must be greater than 0")
        Integer warningMinutes
) {
}
