package xyz.mobi.reportservice;

import org.junit.jupiter.api.Test;
import xyz.mobi.reportservice.dto.*;
import xyz.mobi.reportservice.service.SummaryReportService;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SummaryReportServiceTest {

    private final SummaryReportService summaryReportService = new SummaryReportService();

    @Test
    public void testGenerateJsonSummaryForGeneralSummary() {
        DashboardSummaryDTO summary = new DashboardSummaryDTO(
                15, 3, 5, 2, 5, 0, 0, 1, 1, 0, 14, 93.3, 3.2, 10, 5
        );

        List<DistributionItemDTO> statusDist = List.of(
                new DistributionItemDTO("OPEN", "Open", 3, 20.0),
                new DistributionItemDTO("RESOLVED", "Resolved", 5, 33.3)
        );

        List<DistributionItemDTO> priorityDist = List.of(
                new DistributionItemDTO("HIGH", "High", 7, 46.7),
                new DistributionItemDTO("MEDIUM", "Medium", 8, 53.3)
        );

        DashboardAnalyticsResponseDTO analytics = new DashboardAnalyticsResponseDTO(
                "ADMIN",
                1L,
                "Admin User",
                "admin@helpdesk.com",
                "System Administrator",
                "IT Support",
                1L,
                summary,
                statusDist,
                priorityDist,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                new EmployeeStatsDTO(20, 18, 1, 1),
                Collections.emptyList(),
                Instant.now(),
                "2026-10-01",
                "2026-10-10"
        );

        ReportGenerationPayloadDTO payload = new ReportGenerationPayloadDTO(
                ReportType.GENERAL_SUMMARY,
                ReportFormat.JSON,
                "ADMIN",
                1L,
                "admin@helpdesk.com",
                "Admin User",
                "UTC",
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 10),
                null,
                null,
                null,
                analytics,
                Collections.emptyList(),
                Collections.emptyMap()
        );

        ReportSummaryResponseDTO response = summaryReportService.generateJsonSummary(payload);

        assertNotNull(response);
        assertEquals(ReportType.GENERAL_SUMMARY, response.getReportType());
        assertEquals(15L, response.getTotalRecords());
        assertEquals(3L, response.getSummary().openTickets());
        assertEquals(5L, response.getSummary().resolvedTickets());
        assertNotNull(response.getMetrics());
        assertEquals(93.3, response.getMetrics().get("slaComplianceRate"));
        assertEquals(2, response.getStatusDistribution().size());
    }

    @Test
    public void testGenerateJsonSummaryForSlaSummary() {
        DashboardSummaryDTO summary = new DashboardSummaryDTO(
                8, 1, 2, 0, 5, 0, 0, 2, 2, 1, 6, 75.0, 6.0, 5, 3
        );

        DashboardAnalyticsResponseDTO analytics = new DashboardAnalyticsResponseDTO(
                "EMPLOYEE",
                5L,
                "John Doe",
                "john@helpdesk.com",
                "Software Engineer",
                "Engineering",
                2L,
                summary,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                null,
                Collections.emptyList(),
                Instant.now(),
                "2026-10-01",
                "2026-10-10"
        );

        ReportGenerationPayloadDTO payload = new ReportGenerationPayloadDTO(
                ReportType.SLA_SUMMARY,
                ReportFormat.JSON,
                "EMPLOYEE",
                5L,
                "john@helpdesk.com",
                "John Doe",
                "UTC",
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 10),
                null,
                null,
                null,
                analytics,
                Collections.emptyList(),
                Collections.emptyMap()
        );

        ReportSummaryResponseDTO response = summaryReportService.generateJsonSummary(payload);

        assertNotNull(response);
        assertEquals(ReportType.SLA_SUMMARY, response.getReportType());
        assertEquals(75.0, response.getMetrics().get("slaComplianceRate"));
        assertEquals(2L, response.getMetrics().get("slaBreachedTickets"));
    }
}
