package xyz.mobi.reportservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import xyz.mobi.reportservice.dto.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class SummaryReportService {

    public ReportSummaryResponseDTO generateJsonSummary(ReportGenerationPayloadDTO payload) {
        DashboardAnalyticsResponseDTO analytics = payload.analytics();
        ReportType type = payload.reportType() != null ? payload.reportType() : ReportType.GENERAL_SUMMARY;

        DashboardSummaryDTO summary = analytics != null ? analytics.summary() : null;
        long totalRecords = summary != null ? summary.totalTickets() : 0L;

        Map<String, Object> metrics = new HashMap<>();
        if (summary != null) {
            metrics.put("totalTickets", summary.totalTickets());
            metrics.put("openTickets", summary.openTickets());
            metrics.put("inProgressTickets", summary.inProgressTickets());
            metrics.put("resolvedTickets", summary.resolvedTickets());
            metrics.put("slaBreachedTickets", summary.slaBreachedTickets());
            metrics.put("slaComplianceRate", summary.slaComplianceRate());
            metrics.put("avgResolutionHours", summary.avgResolutionHours());
            metrics.put("assignedTickets", summary.assignedTickets());
            metrics.put("unassignedTickets", summary.unassignedTickets());
        }

        ReportSummaryResponseDTO.ReportSummaryResponseDTOBuilder builder = ReportSummaryResponseDTO.builder()
                .reportType(type)
                .format(ReportFormat.JSON)
                .generatedAt(Instant.now())
                .role(payload.requestedByRole() != null ? payload.requestedByRole() : (analytics != null ? analytics.role() : null))
                .employeeId(payload.requesterEmployeeId() != null ? payload.requesterEmployeeId() : (analytics != null ? analytics.employeeId() : null))
                .employeeName(payload.requesterName() != null ? payload.requesterName() : (analytics != null ? analytics.employeeName() : null))
                .departmentName(analytics != null ? analytics.departmentName() : null)
                .timezone(payload.requesterTimezone() != null ? payload.requesterTimezone() : "UTC")
                .fromDate(payload.fromDate() != null ? payload.fromDate().toString() : (analytics != null ? analytics.fromDate() : null))
                .toDate(payload.toDate() != null ? payload.toDate().toString() : (analytics != null ? analytics.toDate() : null))
                .totalRecords(totalRecords)
                .summary(summary)
                .metrics(metrics);

        // Customize output per ReportType based on domain requirements
        switch (type) {
            case TICKET_SUMMARY -> {
                if (analytics != null) {
                    builder.statusDistribution(analytics.statusDistribution())
                            .priorityDistribution(analytics.priorityDistribution())
                            .categoryDistribution(analytics.categoryDistribution());
                }
            }
            case AGENT_SUMMARY -> {
                if (analytics != null) {
                    builder.agentWorkload(analytics.agentWorkload())
                            .statusDistribution(analytics.statusDistribution());
                }
            }
            case EMPLOYEE_SUMMARY -> {
                if (analytics != null) {
                    builder.employeeStats(analytics.employeeStats())
                            .departmentDistribution(analytics.departmentDistribution());
                }
            }
            case MANAGER_SUMMARY -> {
                if (analytics != null) {
                    builder.departmentDistribution(analytics.departmentDistribution())
                            .agentWorkload(analytics.agentWorkload())
                            .categoryDistribution(analytics.categoryDistribution())
                            .statusDistribution(analytics.statusDistribution());
                }
            }
            case ADMIN_SUMMARY -> {
                if (analytics != null) {
                    builder.departmentDistribution(analytics.departmentDistribution())
                            .agentWorkload(analytics.agentWorkload())
                            .categoryDistribution(analytics.categoryDistribution())
                            .employeeStats(analytics.employeeStats())
                            .statusDistribution(analytics.statusDistribution())
                            .priorityDistribution(analytics.priorityDistribution())
                            .trends(analytics.trends());
                }
            }
            case SLA_SUMMARY -> {
                if (analytics != null) {
                    builder.priorityDistribution(analytics.priorityDistribution())
                            .statusDistribution(analytics.statusDistribution())
                            .trends(analytics.trends());
                }
            }
            case USER_SUMMARY -> {
                if (analytics != null) {
                    builder.statusDistribution(analytics.statusDistribution())
                            .priorityDistribution(analytics.priorityDistribution())
                            .trends(analytics.trends());
                }
            }
            case GENERAL_SUMMARY -> {
                if (analytics != null) {
                    builder.statusDistribution(analytics.statusDistribution())
                            .priorityDistribution(analytics.priorityDistribution())
                            .categoryDistribution(analytics.categoryDistribution())
                            .departmentDistribution(analytics.departmentDistribution())
                            .agentWorkload(analytics.agentWorkload())
                            .employeeStats(analytics.employeeStats())
                            .trends(analytics.trends());
                }
            }
        }

        // Attach sample authorized records (first 10 records) if present
        if (payload.tickets() != null && !payload.tickets().isEmpty()) {
            builder.sampleRecords(payload.tickets().stream().limit(10).toList());
        }

        return builder.build();
    }
}
