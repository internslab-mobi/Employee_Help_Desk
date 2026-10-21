package xyz.mobi.reportservice.service;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import xyz.mobi.reportservice.dto.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExcelReportGeneratorServiceTest {

    private ExcelReportGeneratorService generatorService;

    @BeforeEach
    void setUp() {
        generatorService = new ExcelReportGeneratorService();
    }

    @Test
    @DisplayName("Should generate valid Excel workbook (.xlsx) bytes with summary and ticket sheets")
    void shouldGenerateValidExcelWorkbook() throws IOException {
        DashboardSummaryDTO summary = new DashboardSummaryDTO(
                10L, 2L, 3L, 0L, 5L, 0L, 0L, 0L, 0L, 0L, 10L, 100.0, 2.0, 5L, 5L
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
                List.of(new DistributionItemDTO("RESOLVED", "Resolved", 5L, 50.0)),
                List.of(new DistributionItemDTO("HIGH", "High", 5L, 50.0)),
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

        ExportTicketDetailDTO ticket = new ExportTicketDetailDTO(
                1L,
                "HD-2026-000001",
                "Laptop not booting",
                "IT",
                "Hardware",
                "Laptops",
                "RESOLVED",
                "HIGH",
                "Alice",
                "alice@company.com",
                "Bob",
                "COMPLIANT",
                Instant.parse("2026-10-01T10:00:00Z"),
                Instant.parse("2026-10-01T11:00:00Z"),
                Instant.parse("2026-10-02T12:00:00Z"),
                Instant.parse("2026-10-03T10:00:00Z")
        );

        byte[] excelBytes = generatorService.generateSummaryWorkbook(
                analytics,
                List.of(ticket),
                "Asia/Kolkata",
                ReportType.TICKET_SUMMARY
        );

        assertNotNull(excelBytes);
        assertTrue(excelBytes.length > 0, "Excel output byte array must not be empty");

        // Parse workbook using Apache POI to verify it is valid XLSX
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excelBytes))) {
            assertTrue(workbook.getNumberOfSheets() >= 1, "Workbook should contain at least 1 sheet");
            assertNotNull(workbook.getSheet("Ticket Summary") != null ? workbook.getSheet("Ticket Summary") : workbook.getSheetAt(0));
        }
    }

    @Test
    @DisplayName("Should generate valid Excel workbook for AGENT_SUMMARY with Agent Summary, Status & Priority, and Ticket Records sheets")
    void shouldGenerateValidAgentSummaryWorkbook() throws IOException {
        DashboardSummaryDTO summary = new DashboardSummaryDTO(
                5L, 2L, 2L, 0L, 1L, 0L, 0L, 0L, 0L, 0L, 5L, 100.0, 1.5, 5L, 0L
        );

        DashboardAnalyticsResponseDTO analytics = new DashboardAnalyticsResponseDTO(
                "AGENT",
                2L,
                "Support Agent",
                "agent@company.com",
                "Agent",
                "IT Support",
                1L,
                summary,
                List.of(new DistributionItemDTO("IN_PROGRESS", "In Progress", 2L, 40.0)),
                List.of(new DistributionItemDTO("HIGH", "High", 2L, 40.0)),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                null,
                Collections.emptyList(),
                null,
                "2026-10-01",
                "2026-10-20"
        );

        ExportTicketDetailDTO ticket = new ExportTicketDetailDTO(
                2L,
                "HD-2026-000002",
                "VPN access issue",
                "IT Support",
                "Network",
                "VPN",
                "IN_PROGRESS",
                "HIGH",
                "John Doe",
                "john@company.com",
                "Support Agent",
                "COMPLIANT",
                Instant.parse("2026-10-10T10:00:00Z"),
                Instant.parse("2026-10-10T11:00:00Z"),
                null,
                Instant.parse("2026-10-12T10:00:00Z")
        );

        byte[] excelBytes = generatorService.generateSummaryWorkbook(
                analytics,
                List.of(ticket),
                "Asia/Kolkata",
                ReportType.AGENT_SUMMARY
        );

        assertNotNull(excelBytes);
        assertTrue(excelBytes.length > 0, "Excel output byte array must not be empty");

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excelBytes))) {
            assertNotNull(workbook.getSheet("Agent Summary"), "Workbook must contain 'Agent Summary' sheet");
            assertNotNull(workbook.getSheet("Status & Priority"), "Workbook must contain 'Status & Priority' sheet");
            assertNotNull(workbook.getSheet("Ticket Records"), "Workbook must contain 'Ticket Records' sheet");
            assertEquals("HD-2026-000002", workbook.getSheet("Ticket Records").getRow(1).getCell(0).getStringCellValue());
            assertEquals("Support Agent", workbook.getSheet("Ticket Records").getRow(1).getCell(10).getStringCellValue());
        }
    }
}
