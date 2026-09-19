package com.divya.helpdesk.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AgentSkillAssignRequest {

    @NotNull(message = "Agent ID is required")
    private Long agentId;

    @NotNull(message = "Skill ID is required")
    private Long skillId;
}
