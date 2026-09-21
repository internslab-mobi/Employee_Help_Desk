package com.divya.helpdesk.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AgentSkillResponse {
    private Long id;
    private Long agentId;
    private Long employeeId;
    private String employeeName;
    private Long skillId;
    private String skillName;
    private LocalDateTime createdAt;
}
