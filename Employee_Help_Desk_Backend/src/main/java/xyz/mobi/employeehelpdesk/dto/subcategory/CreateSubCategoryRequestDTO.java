package xyz.mobi.employeehelpdesk.dto.subcategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import xyz.mobi.employeehelpdesk.entity.enums.Priority;

@Builder
public record CreateSubCategoryRequestDTO(
        @NotNull(message = "Category ID is required")
        Long categoryId,

        @NotBlank(message = "SubCategory name is required")
        @Size(max = 150, message = "Name cannot exceed 150 characters")
        String name,

        String description,

        Priority priority,

        java.util.List<Long> skillIds
) {}
