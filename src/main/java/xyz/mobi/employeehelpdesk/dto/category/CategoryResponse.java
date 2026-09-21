package xyz.mobi.employeehelpdesk.dto.category;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record CategoryResponse(
        Long id,
        Long departmentId,
        String departmentName,
        String name,
        String description,
        Boolean isActive,
        Long createdById,
        String createdByName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
