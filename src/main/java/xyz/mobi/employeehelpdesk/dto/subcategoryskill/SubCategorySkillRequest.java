package xyz.mobi.employeehelpdesk.dto.subcategoryskill;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record SubCategorySkillRequest(
        @NotNull(message = "Skill ID is required")
        Long skillId
) {
}
