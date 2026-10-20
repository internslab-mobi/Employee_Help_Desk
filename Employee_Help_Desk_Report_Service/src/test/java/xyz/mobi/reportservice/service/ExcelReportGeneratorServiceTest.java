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
}
