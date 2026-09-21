package xyz.mobi.employeehelpdesk.dto.skill;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record UpdateSkillRequest(
        @NotBlank(message = "Skill name is required")
        @Size(max = 100, message = "Skill name cannot exceed 100 characters")
        String name,

        String description,

        @NotNull(message = "isActive flag is required")
        Boolean isActive
) {
}
