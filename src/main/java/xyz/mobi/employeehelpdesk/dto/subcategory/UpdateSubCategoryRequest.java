package xyz.mobi.employeehelpdesk.dto.subcategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import xyz.mobi.employeehelpdesk.entity.enums.Priority;

@Builder
public record UpdateSubCategoryRequest(
        @NotBlank(message = "SubCategory name is required")
        @Size(max = 150, message = "SubCategory name cannot exceed 150 characters")
        String name,

        String description,

        @NotNull(message = "Priority is required")
        Priority priority,

        @NotNull(message = "isActive flag is required")
        Boolean isActive
) {
}
