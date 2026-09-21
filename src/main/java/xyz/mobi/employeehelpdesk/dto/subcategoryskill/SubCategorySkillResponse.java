package xyz.mobi.employeehelpdesk.dto.subcategoryskill;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record SubCategorySkillResponse(
        Long id,
        Long subCategoryId,
        String subCategoryName,
        Long skillId,
        String skillName,
        LocalDateTime createdAt
) {
}
