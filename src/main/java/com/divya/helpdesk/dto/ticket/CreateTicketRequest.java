package com.divya.helpdesk.dto.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateTicketRequest {

    @NotNull(message = "Department is required")
    private Long departmentId;

    @NotNull(message = "Category is required")
    private Long categoryId;

    @NotNull(message = "Sub category is required")
    private Long subCategoryId;

    @NotBlank(message = "Description is required")
    private String description;
}