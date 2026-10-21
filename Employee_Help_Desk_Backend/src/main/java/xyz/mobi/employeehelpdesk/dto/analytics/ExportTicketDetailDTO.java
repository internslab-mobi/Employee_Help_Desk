package xyz.mobi.employeehelpdesk.dto.analytics;

import lombok.Builder;
import xyz.mobi.employeehelpdesk.entity.enums.Priority;
import xyz.mobi.employeehelpdesk.entity.enums.TicketStatus;

import java.time.Instant;

@Builder
public record ExportTicketDetailDTO(
        Long id,
        String ticketNumber,
        String subject,
        String departmentName,
        String categoryName,
        String subCategoryName,
        TicketStatus status,
        Priority priority,
        String requesterName,
        String requesterEmail,
        String assignedAgentName,
        String slaStatus,
        Instant createdAt,
        Instant updatedAt,
        Instant resolvedAt,
        Instant deadlineAt
) {
}
