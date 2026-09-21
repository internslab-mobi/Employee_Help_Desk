package xyz.mobi.employeehelpdesk.dto.agentskill;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record AgentSkillResponse(
        Long id,
        Long agentId,
        Long employeeId,
        String employeeName,
        Long departmentId,
        String departmentName,
        Long skillId,
        String skillName,
        LocalDateTime createdAt
) {
}
