package com.example.helpdesk.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Data for CATEGORY operation")
public class CategoryUpdateDataRequestDTO implements TicketOperationDataRequestDTO {

    @NotNull(message = "CategoryId is required")
    @Schema(description = "New category ID", example = "2", required = true)
    private Long categoryId;

    @NotNull(message = "SubCategoryId is required")
    @Schema(description = "New subcategory ID", example = "5", required = true)
    private Long subCategoryId;
}
