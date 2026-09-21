package xyz.mobi.employeehelpdesk.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record UpdateCategoryRequest(
        @NotBlank(message = "Category name is required")
        @Size(max = 150, message = "Category name cannot exceed 150 characters")
        String name,

        String description,

        @NotNull(message = "isActive flag is required")
        Boolean isActive
) {
}
