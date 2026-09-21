package com.example.helpdesk.controller;

import com.example.helpdesk.dto.response.*;
import com.example.helpdesk.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ReportSummaryResponse> getDashboardSummary() {
        return ResponseEntity.ok(reportService.getDashboardSummary());
    }

    @GetMapping("/tickets/status")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<TicketCountByStatusResponse>> getTicketCountByStatus() {
        return ResponseEntity.ok(reportService.getTicketCountByStatus());
    }

    @GetMapping("/tickets/priority")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<TicketCountByPriorityResponse>> getTicketCountByPriority() {
        return ResponseEntity.ok(reportService.getTicketCountByPriority());
    }

    @GetMapping("/tickets/department")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<TicketCountByDepartmentResponse>> getTicketCountByDepartment() {
        return ResponseEntity.ok(reportService.getTicketCountByDepartment());
    }

    @GetMapping("/tickets/category")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<TicketCountByCategoryResponse>> getTicketCountByCategory() {
        return ResponseEntity.ok(reportService.getTicketCountByCategory());
    }

    @GetMapping("/tickets/sub-category")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<TicketCountByCategoryResponse>> getTicketCountBySubCategory() {
        return ResponseEntity.ok(reportService.getTicketCountBySubCategory());
    }

    @GetMapping("/tickets/agent")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<TicketCountByAgentResponse>> getTicketCountByAgent() {
        return ResponseEntity.ok(reportService.getTicketCountByAgent());
    }

    @GetMapping("/sla")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ReportSummaryResponse> getSlaSummary() {
        return ResponseEntity.ok(reportService.getDashboardSummary());
    }

    @GetMapping("/feedback")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<FeedbackSummaryResponse> getFeedbackSummary() {
        return ResponseEntity.ok(reportService.getFeedbackSummary());
    }

    @GetMapping("/workload")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<WorkloadResponse>> getAgentWorkload() {
        return ResponseEntity.ok(reportService.getAgentWorkload());
    }

    @GetMapping("/workload/department")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<WorkloadResponse>> getDepartmentWorkload() {
        return ResponseEntity.ok(reportService.getDepartmentWorkload());
    }

    @GetMapping("/tickets/date-range")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<TicketCountByStatusResponse>> getTicketVolumeByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate) {

        return ResponseEntity.ok(reportService.getTicketVolumeByDate(fromDate, toDate));
    }
}
