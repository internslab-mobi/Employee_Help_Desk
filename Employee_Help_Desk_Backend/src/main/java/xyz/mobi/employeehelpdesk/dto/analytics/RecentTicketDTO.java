package xyz.mobi.employeehelpdesk.dto.analytics;

import lombok.Builder;
import xyz.mobi.employeehelpdesk.entity.enums.Priority;
import xyz.mobi.employeehelpdesk.entity.enums.TicketStatus;

import java.time.Instant;

@Builder
public record RecentTicketDTO(
        Long id,
        String ticketNumber,
        String subject,
        String categoryName,
        String subCategoryName,
        TicketStatus status,
        Priority priority,
        String slaStatus,
        String requesterName,
        String assignedAgentName,
        String departmentName,
        Instant createdAt,
        Instant resolvedAt,
        Instant currentDeadlineAt
) {
}
