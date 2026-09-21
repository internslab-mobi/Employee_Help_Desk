package xyz.mobi.employeehelpdesk.dto.subcategory;

import lombok.Builder;
import xyz.mobi.employeehelpdesk.entity.enums.Priority;

import java.time.LocalDateTime;

@Builder
public record SubCategoryResponse(
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
        String createdByName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
