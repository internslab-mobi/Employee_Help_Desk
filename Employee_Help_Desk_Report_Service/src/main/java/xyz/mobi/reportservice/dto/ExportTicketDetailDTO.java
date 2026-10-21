package xyz.mobi.reportservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExportTicketDetailDTO(
        Long id,
        String ticketNumber,
        String subject,
        String departmentName,
        String categoryName,
        String subCategoryName,
        String status,
        String priority,
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
