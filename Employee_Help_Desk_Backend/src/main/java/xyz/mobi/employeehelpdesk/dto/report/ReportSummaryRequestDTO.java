package xyz.mobi.employeehelpdesk.dto.report;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import xyz.mobi.employeehelpdesk.entity.enums.Priority;
import xyz.mobi.employeehelpdesk.entity.enums.TicketStatus;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Request criteria for generating summary reports")
public class ReportSummaryRequestDTO {

    @Builder.Default
    @Schema(description = "Report category type", defaultValue = "GENERAL_SUMMARY")
    private ReportType reportType = ReportType.GENERAL_SUMMARY;

    @Builder.Default
    @Schema(description = "Desired output format (JSON or EXCEL)", defaultValue = "JSON")
    private ReportFormat format = ReportFormat.JSON;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Schema(description = "Start date filter (inclusive)", example = "2026-10-01")
    private LocalDate fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Schema(description = "End date filter (inclusive)", example = "2026-10-12")
    private LocalDate toDate;

    @Schema(description = "Optional department filter (restricted to authorized scope)", example = "1")
    private Long departmentId;

    @Schema(description = "Optional ticket status filter", example = "OPEN")
    private TicketStatus status;

    @Schema(description = "Optional ticket priority filter", example = "HIGH")
    private Priority priority;

    @Schema(description = "Optional employee filter (subject to role authorization)", example = "1")
    private Long employeeId;

    @AssertTrue(message = "fromDate cannot be after toDate")
    public boolean isDateRangeValid() {
        if (fromDate != null && toDate != null) {
            return !fromDate.isAfter(toDate);
        }
        return true;
    }
}
