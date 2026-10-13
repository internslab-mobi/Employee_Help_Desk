package xyz.mobi.employeehelpdesk.dto.report;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import xyz.mobi.employeehelpdesk.dto.analytics.DashboardAnalyticsResponseDTO;
import xyz.mobi.employeehelpdesk.dto.analytics.ExportTicketDetailDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public record ReportGenerationPayloadDTO(
        ReportType reportType,
        ReportFormat format,
        String requestedByRole,
        Long requesterEmployeeId,
        String requesterEmail,
        String requesterName,
        String requesterTimezone,
        LocalDate fromDate,
        LocalDate toDate,
        Long departmentId,
        String status,
        String priority,
        DashboardAnalyticsResponseDTO analytics,
        List<ExportTicketDetailDTO> tickets,
        Map<String, Object> additionalFilters
) {
}
