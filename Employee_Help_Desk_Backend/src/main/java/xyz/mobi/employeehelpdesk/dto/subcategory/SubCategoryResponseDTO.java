package xyz.mobi.employeehelpdesk.dto.subcategory;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import xyz.mobi.employeehelpdesk.entity.enums.Priority;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SubCategoryResponseDTO(
        Long id,
        Long categoryId,
        String categoryName,
        Long departmentId,
        String departmentName,
        String name,
        String description,
        Priority priority,
        Boolean isActive,
        Long createdById,
        String createdByName
) {
}
