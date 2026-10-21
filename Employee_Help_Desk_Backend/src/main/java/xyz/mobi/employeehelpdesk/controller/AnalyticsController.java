package xyz.mobi.employeehelpdesk.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import xyz.mobi.employeehelpdesk.dto.analytics.DashboardAnalyticsResponseDTO;
import xyz.mobi.employeehelpdesk.dto.analytics.ExportTicketDetailDTO;
import xyz.mobi.employeehelpdesk.service.AnalyticsService;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Analytics", description = "Role-based Dashboard Analytics & Export Data APIs")
@RestController
@RequestMapping("/helpdesk/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @Operation(summary = "Get role-scoped dashboard analytics")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardAnalyticsResponseDTO> getDashboardAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long departmentId
    ) {
        DashboardAnalyticsResponseDTO response = analyticsService.getDashboardAnalytics(fromDate, toDate, departmentId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get role-scoped ticket records for report export")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    @GetMapping("/export-data")
    public ResponseEntity<List<ExportTicketDetailDTO>> getExportTicketDetails(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long departmentId
    ) {
        List<ExportTicketDetailDTO> response = analyticsService.getExportTicketDetails(fromDate, toDate, departmentId);
        return ResponseEntity.ok(response);
    }
}
