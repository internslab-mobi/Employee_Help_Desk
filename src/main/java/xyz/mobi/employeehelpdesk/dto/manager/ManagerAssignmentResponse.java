package xyz.mobi.employeehelpdesk.dto.manager;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ManagerAssignmentResponse(
        Long id,
        Long departmentId,
        String departmentName,
        Long employeeId,
        String employeeCode,
        String employeeName,
        String email,
        Boolean isPrimary,
        LocalDateTime createdAt
) {
}
