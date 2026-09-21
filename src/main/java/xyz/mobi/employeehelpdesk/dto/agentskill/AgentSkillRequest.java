package xyz.mobi.employeehelpdesk.dto.agentskill;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record AgentSkillRequest(
        @NotNull(message = "Skill ID is required")
        Long skillId
) {
}
