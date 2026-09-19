package com.example.helpdesk.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTicketCategoryRequest {

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @NotNull(message = "Sub-category ID is required")
    private Long subCategoryId;

}
