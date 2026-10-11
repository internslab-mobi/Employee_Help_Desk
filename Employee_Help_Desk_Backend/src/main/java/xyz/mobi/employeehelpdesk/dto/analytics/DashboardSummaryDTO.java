package xyz.mobi.employeehelpdesk.dto.analytics;

import lombok.Builder;

@Builder
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
