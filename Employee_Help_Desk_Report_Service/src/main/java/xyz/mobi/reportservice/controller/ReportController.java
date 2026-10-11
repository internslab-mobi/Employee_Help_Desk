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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import xyz.mobi.reportservice.dto.DashboardAnalyticsResponseDTO;
import xyz.mobi.reportservice.dto.ExportTicketDetailDTO;
import xyz.mobi.reportservice.security.ReportUserPrincipal;
import xyz.mobi.reportservice.service.BackendClientService;
import xyz.mobi.reportservice.service.ExcelReportGeneratorService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final BackendClientService backendClientService;
    private final ExcelReportGeneratorService excelReportGeneratorService;

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
