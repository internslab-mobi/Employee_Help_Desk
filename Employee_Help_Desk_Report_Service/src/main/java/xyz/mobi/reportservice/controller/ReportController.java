package xyz.mobi.reportservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import xyz.mobi.reportservice.dto.*;
import xyz.mobi.reportservice.security.ReportUserPrincipal;
import xyz.mobi.reportservice.service.BackendClientService;
import xyz.mobi.reportservice.service.ExcelReportGeneratorService;
import xyz.mobi.reportservice.service.SummaryReportService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/helpdesk/reports")
@RequiredArgsConstructor
public class ReportController {

    private final BackendClientService backendClientService;
    private final ExcelReportGeneratorService excelReportGeneratorService;
    private final SummaryReportService summaryReportService;

    /**
     * Generic report generation endpoint invoked by Core Backend.
     * Supports both on-screen JSON summary and Excel workbook (.xlsx) generation.
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/generate")
    public ResponseEntity<?> generateReport(
            @AuthenticationPrincipal ReportUserPrincipal principal,
            @RequestBody ReportGenerationPayloadDTO payload
    ) {
        if (principal == null && payload == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            ReportFormat format = payload.format() != null ? payload.format() : ReportFormat.JSON;

            if (format == ReportFormat.EXCEL) {
                byte[] excelBytes = excelReportGeneratorService.generateSummaryWorkbook(
                        payload.analytics(),
                        payload.tickets() != null ? payload.tickets() : Collections.emptyList(),
                        payload.requesterTimezone(),
                        payload.reportType()
                );

                String todayStr = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
                String rolePrefix = (payload.requestedByRole() != null ? payload.requestedByRole() : "Report");
                String typePrefix = (payload.reportType() != null ? payload.reportType().name() : "Summary");
                String filename = rolePrefix + "_" + typePrefix + "_" + todayStr + ".xlsx";

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
                headers.setContentDispositionFormData("attachment", filename);
                headers.set("filename", filename);
                headers.setContentLength(excelBytes.length);

                return ResponseEntity.ok()
                        .headers(headers)
                        .body(excelBytes);
            } else {
                ReportSummaryResponseDTO response = summaryReportService.generateJsonSummary(payload);
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(response);
            }
        } catch (Exception ex) {
            log.error("Failed to generate report in microservice: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Error-Message", ex.getMessage())
                    .body(Collections.singletonMap("error", "Microservice report generation failed: " + ex.getMessage()));
        }
    }

    /**
     * Direct legacy export endpoint.
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/summary/export")
    public ResponseEntity<byte[]> exportSummaryToExcel(
            @AuthenticationPrincipal ReportUserPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long departmentId
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            DashboardAnalyticsResponseDTO analytics = backendClientService.fetchDashboardAnalytics(
                    principal.getRawToken(), fromDate, toDate, departmentId
            );

            List<ExportTicketDetailDTO> tickets = backendClientService.fetchExportTicketDetails(
                    principal.getRawToken(), fromDate, toDate, departmentId
            );

            byte[] excelBytes = excelReportGeneratorService.generateSummaryWorkbook(
                    analytics, tickets, principal.getTimezone()
            );

            String todayStr = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
            String rolePrefix = (principal.getRole() != null ? principal.getRole() : "Employee");
            String filename = rolePrefix + "_Ticket_Summary_" + todayStr + ".xlsx";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", filename);
            headers.set("filename", filename);
            headers.setContentLength(excelBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelBytes);

        } catch (Exception ex) {
            log.error("Failed to generate Excel summary report: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Error-Message", ex.getMessage())
                    .build();
        }
    }
}
