package xyz.mobi.reportservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
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
