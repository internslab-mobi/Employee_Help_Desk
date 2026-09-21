package xyz.mobi.employeehelpdesk.dto.agent;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record AgentAssignmentResponse(
        Long id,
        Long departmentId,
        String departmentName,
        Long employeeId,
        String employeeCode,
        String employeeName,
        String email,
        LocalDateTime lastAssignedAt,
        LocalDateTime createdAt
) {
}
