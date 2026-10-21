package xyz.mobi.employeehelpdesk.dto.analytics;

import lombok.Builder;

@Builder
public record AgentWorkloadDTO(
        Long agentId,
        Long employeeId,
        String agentName,
        String email,
        String departmentName,
        long activeTickets,
        long resolvedTickets,
        long totalTickets
) {
}
