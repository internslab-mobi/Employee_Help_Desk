package xyz.mobi.reportservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DashboardSummaryDTO(
        long totalTickets,
        long openTickets,
        long inProgressTickets,
        long onHoldTickets,
        long resolvedTickets,
        long withdrawnTickets,
        long reopenedTickets,
        long overdueTickets,
        long slaBreachedTickets,
        long slaWarningTickets,
        long slaCompliantTickets,
        double slaComplianceRate,
        double avgResolutionHours,
        long assignedTickets,
        long unassignedTickets
) {
}
