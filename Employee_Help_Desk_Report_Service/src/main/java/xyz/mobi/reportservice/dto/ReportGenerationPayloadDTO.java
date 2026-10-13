package xyz.mobi.reportservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
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
