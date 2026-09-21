package xyz.mobi.employeehelpdesk.dto.slapolicy;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record SlaPolicyResponse(
        Long id,
        Long departmentId,
        String departmentName,
        Long subCategoryId,
        String subCategoryName,
        Integer durationMinutes,
        Integer warningMinutes,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
