package xyz.mobi.reportservice;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import xyz.mobi.reportservice.dto.*;
import xyz.mobi.reportservice.service.ExcelReportGeneratorService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ExcelReportGeneratorServiceTest {

    private final ExcelReportGeneratorService generatorService = new ExcelReportGeneratorService();

    @Test
    public void testGenerateValidXlsxWorkbook() throws IOException {
        DashboardSummaryDTO summary = new DashboardSummaryDTO(
                10, 2, 3, 1, 4, 0, 0, 1, 1, 0, 9, 90.0, 4.5, 8, 2
        );

        List<DistributionItemDTO> statusDist = List.of(
                new DistributionItemDTO("OPEN", "Open", 2, 20.0),
                new DistributionItemDTO("RESOLVED", "Resolved", 4, 40.0)
        );

        List<DistributionItemDTO> priorityDist = List.of(
                new DistributionItemDTO("HIGH", "High", 5, 50.0),
                new DistributionItemDTO("MEDIUM", "Medium", 5, 50.0)
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
                new EmployeeStatsDTO(10, 8, 1, 1),
                Collections.emptyList(),
                Instant.now(),
                "2026-10-01",
                "2026-10-10"
        );

        ExportTicketDetailDTO ticket = new ExportTicketDetailDTO(
                1L,
                "TKT-001",
                "Network issue",
                "IT",
                "Hardware",
                "Router",
                "OPEN",
                "HIGH",
                "John Doe",
                "john@example.com",
                "Agent Smith",
                "ACTIVE",
                Instant.now(),
                Instant.now(),
                null,
                null
        );

        byte[] bytes = generatorService.generateSummaryWorkbook(analytics, List.of(ticket), "UTC");

        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        // Verify valid POI workbook parsing
        try (XSSFWorkbook parsed = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertEquals(4, parsed.getNumberOfSheets()); // Executive Summary, Status & Priority, Department & Category, Ticket Records
            assertNotNull(parsed.getSheet("Executive Summary"));
            assertNotNull(parsed.getSheet("Ticket Records"));
        }
    }
}
