package com.divya.helpdesk.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SlaPolicyRequest {

    @NotNull(message = "Department ID is required")
    private Long departmentId;

    @NotNull(message = "SubCategory ID is required")
    private Long subCategoryId;

    @NotNull(message = "Duration in minutes is required")
    @Min(value = 1, message = "Duration must be at least 1 minute")
    private Integer durationMinutes;

    private Integer warningMinutes;

    private Boolean isActive = true;
}
