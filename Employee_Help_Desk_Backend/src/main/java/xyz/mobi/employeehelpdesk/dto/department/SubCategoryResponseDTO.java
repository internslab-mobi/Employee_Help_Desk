package xyz.mobi.employeehelpdesk.dto.department;

import xyz.mobi.employeehelpdesk.entity.enums.Priority;

public record SubCategoryResponseDTO(
        Long id,
        Long categoryId,
        String name,
        String description,
        Priority priority
) {
}
