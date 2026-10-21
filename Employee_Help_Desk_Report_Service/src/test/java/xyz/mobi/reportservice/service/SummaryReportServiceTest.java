package xyz.mobi.reportservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import xyz.mobi.reportservice.dto.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SummaryReportServiceTest {

    private SummaryReportService summaryReportService;

    @BeforeEach
    void setUp() {
        summaryReportService = new SummaryReportService();
    }

    @Test
    @DisplayName("Should generate JSON summary report with valid metrics and summary calculations")
    void shouldGenerateJsonSummaryReport() {
        DashboardSummaryDTO summary = new DashboardSummaryDTO(
                100L, 20L, 30L, 0L, 50L, 0L, 0L, 5L, 5L, 5L, 90L, 95.0, 4.5, 40L, 10L
        );

        DashboardAnalyticsResponseDTO analytics = new DashboardAnalyticsResponseDTO(
                "ADMIN",
                1L,
                "Admin User",
                "admin@company.com",
                "Administrator",
                "IT",
                1L,
                summary,
                List.of(new DistributionItemDTO("OPEN", "Open", 20L, 20.0)),
                List.of(new DistributionItemDTO("HIGH", "High", 30L, 30.0)),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                null,
                Collections.emptyList(),
                null,
                "2026-10-01",
                "2026-10-15"
        );

        ReportGenerationPayloadDTO payload = new ReportGenerationPayloadDTO(
                ReportType.TICKET_SUMMARY,
                ReportFormat.JSON,
                "ADMIN",
                1L,
                "admin@company.com",
                "Admin User",
                "Asia/Kolkata",
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 15),
                1L,
                null,
                null,
                analytics,
                Collections.emptyList(),
                null
        );

        ReportSummaryResponseDTO response = summaryReportService.generateJsonSummary(payload);

        assertNotNull(response);
        assertEquals(ReportType.TICKET_SUMMARY, response.getReportType());
        assertEquals(ReportFormat.JSON, response.getFormat());
        assertEquals("ADMIN", response.getRole());
        assertEquals(100L, response.getTotalRecords());
        assertEquals(95.0, response.getSummary().slaComplianceRate());
        assertEquals(1, response.getStatusDistribution().size());
        assertEquals("OPEN", response.getStatusDistribution().get(0).key());
        assertEquals(1, response.getPriorityDistribution().size());
        assertEquals("HIGH", response.getPriorityDistribution().get(0).key());
        assertTrue(response.getMetrics().containsKey("slaComplianceRate"));
        assertEquals(95.0, response.getMetrics().get("slaComplianceRate"));
    }
}
