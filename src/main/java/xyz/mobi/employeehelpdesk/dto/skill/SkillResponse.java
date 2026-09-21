package xyz.mobi.employeehelpdesk.dto.skill;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record SkillResponse(
        Long id,
        String name,
        String description,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
