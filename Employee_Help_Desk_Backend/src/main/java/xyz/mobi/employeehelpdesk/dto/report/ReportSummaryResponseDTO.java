package xyz.mobi.employeehelpdesk.dto.report;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.mobi.employeehelpdesk.dto.analytics.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReportSummaryResponseDTO {
    private ReportType reportType;
    private ReportFormat format;
    private Instant generatedAt;
    private String role;
    private Long employeeId;
    private String employeeName;
    private String departmentName;
    private String timezone;
    private String fromDate;
    private String toDate;
    private long totalRecords;

    private DashboardSummaryDTO summary;
    private List<DistributionItemDTO> statusDistribution;
    private List<DistributionItemDTO> priorityDistribution;
    private List<DistributionItemDTO> categoryDistribution;
    private List<DepartmentMetricDTO> departmentDistribution;
    private List<AgentWorkloadDTO> agentWorkload;
    private EmployeeStatsDTO employeeStats;
    private List<TrendItemDTO> trends;
    private List<ExportTicketDetailDTO> sampleRecords;
    private Map<String, Object> metrics;
}
