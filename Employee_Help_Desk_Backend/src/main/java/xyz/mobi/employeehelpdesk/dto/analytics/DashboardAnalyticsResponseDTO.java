package xyz.mobi.employeehelpdesk.dto.analytics;

import lombok.Builder;
import xyz.mobi.employeehelpdesk.entity.enums.UserRole;

import java.time.Instant;
import java.util.List;

@Builder
public record DashboardAnalyticsResponseDTO(
        UserRole role,
        Long employeeId,
        String employeeName,
        String employeeEmail,
        String designation,
        String departmentName,
        Long departmentId,
        DashboardSummaryDTO summary,
        List<DistributionItemDTO> statusDistribution,
        List<DistributionItemDTO> priorityDistribution,
        List<DepartmentMetricDTO> departmentDistribution,
        List<DistributionItemDTO> categoryDistribution,
        List<TrendItemDTO> trends,
        List<AgentWorkloadDTO> agentWorkload,
        EmployeeStatsDTO employeeStats,
        List<RecentTicketDTO> recentTickets,
        Instant generatedAt,
        String fromDate,
        String toDate
) {
}
