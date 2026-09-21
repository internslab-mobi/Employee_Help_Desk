package xyz.mobi.employeehelpdesk.dto.department;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record DepartmentResponse(
        Long id,
        String code,
        String name,
        String description,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
