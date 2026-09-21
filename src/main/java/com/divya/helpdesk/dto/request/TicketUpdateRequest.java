package com.divya.helpdesk.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TicketUpdateRequest {

    @NotNull(message = "Department id is required")
    private Long departmentId;

    @NotNull(message = "Category id is required")
    private Long categoryId;

    @NotNull(message = "Sub-category id is required")
    private Long subCategoryId;

    @NotBlank(message = "Subject is required")
    @Size(max = 255, message = "Subject must not exceed 255 characters")
    private String subject;

    @NotBlank(message = "Description is required")
    private String description;
}