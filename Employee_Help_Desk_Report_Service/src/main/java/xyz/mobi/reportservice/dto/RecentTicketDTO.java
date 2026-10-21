package xyz.mobi.reportservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RecentTicketDTO(
        Long id,
        String ticketNumber,
        String subject,
        String categoryName,
        String subCategoryName,
        String status,
        String priority,
        String slaStatus,
        String requesterName,
        String assignedAgentName,
        String departmentName,
        Instant createdAt,
        Instant resolvedAt,
        Instant currentDeadlineAt
) {
}
