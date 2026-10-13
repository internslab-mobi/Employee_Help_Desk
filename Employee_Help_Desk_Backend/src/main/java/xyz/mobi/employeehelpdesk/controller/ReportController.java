package xyz.mobi.employeehelpdesk.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import xyz.mobi.employeehelpdesk.dto.report.ReportSummaryRequestDTO;
import xyz.mobi.employeehelpdesk.service.ReportIntegrationService;

@Slf4j
@Tag(name = "Reports", description = "Summary Report Generation & Excel Export Integration APIs")
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportIntegrationService reportIntegrationService;

    @Operation(summary = "Generate generic summary report (JSON or Excel) through Report Microservice")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    @GetMapping(value = "/summary", produces = {
            MediaType.APPLICATION_JSON_VALUE,
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    })
    public ResponseEntity<?> getSummaryReport(
            @Valid @ModelAttribute ReportSummaryRequestDTO request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader
    ) {
        log.info("Received summary report request: type={}, format={}, from={}, to={}",
                request.getReportType(), request.getFormat(), request.getFromDate(), request.getToDate());

        return reportIntegrationService.getSummaryReport(request, authHeader);
    }
}
