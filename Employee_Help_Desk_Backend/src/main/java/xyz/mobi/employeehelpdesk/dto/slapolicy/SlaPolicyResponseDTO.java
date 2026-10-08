package xyz.mobi.employeehelpdesk.dto.slapolicy;

import lombok.Builder;

@Builder
public record SlaPolicyResponseDTO(
        Long id,
        Long departmentId,
        String departmentName,
        Long subCategoryId,
        String subCategoryName,
        Integer durationMinutes,
        Integer warningMinutes,
        Boolean isActive
) {
}
