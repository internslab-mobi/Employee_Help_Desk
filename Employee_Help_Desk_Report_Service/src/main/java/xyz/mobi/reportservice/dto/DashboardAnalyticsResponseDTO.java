package xyz.mobi.reportservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DashboardAnalyticsResponseDTO(
        String role,
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
