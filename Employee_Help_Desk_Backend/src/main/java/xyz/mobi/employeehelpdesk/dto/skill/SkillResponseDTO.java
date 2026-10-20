package xyz.mobi.employeehelpdesk.dto.skill;

import lombok.Builder;

@Builder
public record SkillResponseDTO(
        Long id,
        String name,
        String description,
        Boolean isActive
) {}
