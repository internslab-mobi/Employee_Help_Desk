package xyz.mobi.reportservice.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import xyz.mobi.reportservice.dto.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class ExcelReportGeneratorService {

    private static final byte[] COLOR_PRIMARY_NAVY = new byte[]{(byte) 30, (byte) 58, (byte) 138}; // #1E3A8A
    private static final byte[] COLOR_SECTION_BG = new byte[]{(byte) 219, (byte) 234, (byte) 254}; // #DBEAFE
    private static final byte[] COLOR_ZEBRA_ROW = new byte[]{(byte) 248, (byte) 250, (byte) 252};  // #F8FAFC
    private static final byte[] COLOR_TOTAL_ROW = new byte[]{(byte) 241, (byte) 245, (byte) 249};  // #F1F5F9

    public byte[] generateSummaryWorkbook(
            DashboardAnalyticsResponseDTO analytics,
            List<ExportTicketDetailDTO> tickets,
            String requesterTimezone
    ) throws IOException {
        return generateSummaryWorkbook(analytics, tickets, requesterTimezone, ReportType.GENERAL_SUMMARY);
    }

    public byte[] generateSummaryWorkbook(
            DashboardAnalyticsResponseDTO analytics,
            List<ExportTicketDetailDTO> tickets,
            String requesterTimezone,
            ReportType reportType
    ) throws IOException {
        ReportType type = reportType != null ? reportType : ReportType.GENERAL_SUMMARY;
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            ZoneId zoneId = ZoneId.of(requesterTimezone != null && !requesterTimezone.isBlank() ? requesterTimezone.trim() : "UTC");
            DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(zoneId);

            StylePalette styles = createStylePalette(workbook);

            switch (type) {
                case TICKET_SUMMARY -> {
                    createTicketOverviewSheet(workbook, analytics, styles, dtFormatter);
                    createTicketRecordsSheet(workbook, tickets, styles, dtFormatter);
                }
                case USER_SUMMARY -> {
                    createUserOverviewSheet(workbook, analytics, styles, dtFormatter);
                    if (analytics != null && analytics.departmentDistribution() != null && !analytics.departmentDistribution().isEmpty()) {
                        createDepartmentCategorySheet(workbook, analytics, styles);
                    }
                    if (analytics != null && analytics.agentWorkload() != null && !analytics.agentWorkload().isEmpty()) {
                        createAgentWorkloadSheet(workbook, analytics, styles);
                    }
                }
                case SLA_SUMMARY -> {
                    createSlaSummarySheet(workbook, analytics, styles, dtFormatter);
                    createSlaTicketRecordsSheet(workbook, tickets, styles, dtFormatter);
                }
                case AGENT_SUMMARY -> {
                    createAgentOverviewSheet(workbook, analytics, styles, dtFormatter);
                    createStatusPrioritySheet(workbook, analytics, styles);
                    createTicketRecordsSheet(workbook, tickets, styles, dtFormatter);
                }
                default -> {
                    // GENERAL_SUMMARY and others
                    createExecutiveSummarySheet(workbook, analytics, styles, dtFormatter);
                    createStatusPrioritySheet(workbook, analytics, styles);
                    if (analytics != null && ("ADMIN".equalsIgnoreCase(analytics.role()) || "MANAGER".equalsIgnoreCase(analytics.role()))) {
                        createDepartmentCategorySheet(workbook, analytics, styles);
                    }
                    if (analytics != null && analytics.agentWorkload() != null && !analytics.agentWorkload().isEmpty()) {
                        createAgentWorkloadSheet(workbook, analytics, styles);
                    }
                    createTicketRecordsSheet(workbook, tickets, styles, dtFormatter);
                }
            }

            // Auto-size all sheets safely
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet s = workbook.getSheetAt(i);
                int maxCols = 0;
                for (Row r : s) {
                    if (r.getLastCellNum() > maxCols) maxCols = r.getLastCellNum();
                }
                for (int c = 0; c < maxCols; c++) {
                    s.autoSizeColumn(c);
                    int currentWidth = s.getColumnWidth(c);
                    s.setColumnWidth(c, Math.min(Math.max(currentWidth + 1200, 3200), 18000));
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void createAgentOverviewSheet(
            XSSFWorkbook wb, DashboardAnalyticsResponseDTO data, StylePalette s, DateTimeFormatter dtf
    ) {
        Sheet sheet = wb.createSheet("Agent Summary");
        sheet.setDisplayGridlines(true);

        int rowNum = 0;

        // Title Banner
        Row titleRow = sheet.createRow(rowNum++);
        titleRow.setHeightInPoints(34);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("EMPLOYEE HELP DESK - AGENT ASSIGNED TICKETS SUMMARY");
        titleCell.setCellStyle(s.titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

        // Subtitle / Report Info
        rowNum++;
        Row infoRow1 = sheet.createRow(rowNum++);
        infoRow1.createCell(0).setCellValue("Generated At:");
        infoRow1.getCell(0).setCellStyle(s.boldLabelStyle);
        infoRow1.createCell(1).setCellValue(dtf.format(Instant.now()));
        infoRow1.getCell(1).setCellStyle(s.regularStyle);

        infoRow1.createCell(3).setCellValue("Report Scope / Role:");
        infoRow1.getCell(3).setCellStyle(s.boldLabelStyle);
        infoRow1.createCell(4).setCellValue(data != null && data.role() != null ? data.role() : "AGENT");
        infoRow1.getCell(4).setCellStyle(s.regularStyle);

        Row infoRow2 = sheet.createRow(rowNum++);
        infoRow2.createCell(0).setCellValue("Support Agent:");
        infoRow2.getCell(0).setCellStyle(s.boldLabelStyle);
        String agentInfo = (data != null && data.employeeName() != null ? data.employeeName() : "Support Agent") +
                (data != null && data.employeeEmail() != null ? " (" + data.employeeEmail() + ")" : "");
        infoRow2.createCell(1).setCellValue(agentInfo);
        infoRow2.getCell(1).setCellStyle(s.regularStyle);

        infoRow2.createCell(3).setCellValue("Department:");
        infoRow2.getCell(3).setCellStyle(s.boldLabelStyle);
        infoRow2.createCell(4).setCellValue(data != null && data.departmentName() != null && !data.departmentName().isBlank() ? data.departmentName() : "Support");
        infoRow2.getCell(4).setCellStyle(s.regularStyle);

        Row infoRow3 = sheet.createRow(rowNum++);
        infoRow3.createCell(0).setCellValue("Date Filter Range:");
        infoRow3.getCell(0).setCellStyle(s.boldLabelStyle);
        String dateRangeText = (data != null && data.fromDate() != null ? data.fromDate() : "Start") + " to " + (data != null && data.toDate() != null ? data.toDate() : "Current");
        infoRow3.createCell(1).setCellValue(dateRangeText);
        infoRow3.getCell(1).setCellStyle(s.regularStyle);

        rowNum += 2;

        // Section: Key Performance Indicators
        Row kpiHeaderRow = sheet.createRow(rowNum++);
        kpiHeaderRow.setHeightInPoints(24);
        Cell kpiHeaderCell = kpiHeaderRow.createCell(0);
        kpiHeaderCell.setCellValue("ASSIGNED TICKETS METRICS OVERVIEW");
        kpiHeaderCell.setCellStyle(s.sectionHeaderStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 3));

        DashboardSummaryDTO sum = data != null ? data.summary() : null;
        if (sum != null) {
            String[][] metrics = {
                    {"Total Assigned Tickets", String.valueOf(sum.totalTickets())},
                    {"Open Tickets", String.valueOf(sum.openTickets())},
                    {"In Progress Tickets", String.valueOf(sum.inProgressTickets())},
                    {"On Hold Tickets", String.valueOf(sum.onHoldTickets())},
                    {"Resolved Tickets", String.valueOf(sum.resolvedTickets())},
                    {"Withdrawn Tickets", String.valueOf(sum.withdrawnTickets())},
                    {"Reopened Tickets", String.valueOf(sum.reopenedTickets())},
                    {"Overdue / SLA Breached", String.valueOf(sum.overdueTickets())},
                    {"SLA Compliance Rate", sum.slaComplianceRate() + "%"},
                    {"Average Resolution Time", sum.avgResolutionHours() + " hours"}
            };

            for (int i = 0; i < metrics.length; i++) {
                Row r = sheet.createRow(rowNum++);
                Cell cLabel = r.createCell(0);
                cLabel.setCellValue(metrics[i][0]);
                cLabel.setCellStyle(i % 2 == 0 ? s.zebraStyle : s.regularStyle);

                Cell cVal = r.createCell(1);
                cVal.setCellValue(metrics[i][1]);
                cVal.setCellStyle(i % 2 == 0 ? s.zebraStyle : s.regularStyle);
            }
        }
    }

    private void createExecutiveSummarySheet(
            XSSFWorkbook wb, DashboardAnalyticsResponseDTO data, StylePalette s, DateTimeFormatter dtf
    ) {
        Sheet sheet = wb.createSheet("Executive Summary");
        sheet.setDisplayGridlines(true);

        int rowNum = 0;

        // Title Banner
        Row titleRow = sheet.createRow(rowNum++);
        titleRow.setHeightInPoints(34);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("EMPLOYEE HELP DESK - EXECUTIVE SUMMARY REPORT");
        titleCell.setCellStyle(s.titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

        // Subtitle / Report Info
        rowNum++;
        Row infoRow1 = sheet.createRow(rowNum++);
        infoRow1.createCell(0).setCellValue("Generated At:");
        infoRow1.getCell(0).setCellStyle(s.boldLabelStyle);
        infoRow1.createCell(1).setCellValue(dtf.format(Instant.now()));
        infoRow1.getCell(1).setCellStyle(s.regularStyle);

        infoRow1.createCell(3).setCellValue("Report Scope / Role:");
        infoRow1.getCell(3).setCellStyle(s.boldLabelStyle);
        infoRow1.createCell(4).setCellValue(data.role() != null ? data.role() : "N/A");
        infoRow1.getCell(4).setCellStyle(s.regularStyle);

        Row infoRow2 = sheet.createRow(rowNum++);
        infoRow2.createCell(0).setCellValue("Generated By:");
        infoRow2.getCell(0).setCellStyle(s.boldLabelStyle);
        infoRow2.createCell(1).setCellValue(data.employeeName() + " (" + data.employeeEmail() + ")");
        infoRow2.getCell(1).setCellStyle(s.regularStyle);

        infoRow2.createCell(3).setCellValue("Department:");
        infoRow2.getCell(3).setCellStyle(s.boldLabelStyle);
        infoRow2.createCell(4).setCellValue(data.departmentName() != null && !data.departmentName().isBlank() ? data.departmentName() : "All Departments");
        infoRow2.getCell(4).setCellStyle(s.regularStyle);

        Row infoRow3 = sheet.createRow(rowNum++);
        infoRow3.createCell(0).setCellValue("Date Filter Range:");
        infoRow3.getCell(0).setCellStyle(s.boldLabelStyle);
        String dateRangeText = (data.fromDate() != null ? data.fromDate() : "Start") + " to " + (data.toDate() != null ? data.toDate() : "Current");
        infoRow3.createCell(1).setCellValue(dateRangeText);
        infoRow3.getCell(1).setCellStyle(s.regularStyle);

        rowNum += 2;

        // Section: Key Performance Indicators
        Row kpiHeaderRow = sheet.createRow(rowNum++);
        kpiHeaderRow.setHeightInPoints(24);
        Cell kpiHeaderCell = kpiHeaderRow.createCell(0);
        kpiHeaderCell.setCellValue("KEY METRICS OVERVIEW");
        kpiHeaderCell.setCellStyle(s.sectionHeaderStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 3));

        DashboardSummaryDTO sum = data.summary();
        if (sum != null) {
            String[][] metrics = {
                    {"Total Tickets", String.valueOf(sum.totalTickets())},
                    {"Open Tickets", String.valueOf(sum.openTickets())},
                    {"In Progress Tickets", String.valueOf(sum.inProgressTickets())},
                    {"On Hold Tickets", String.valueOf(sum.onHoldTickets())},
                    {"Resolved Tickets", String.valueOf(sum.resolvedTickets())},
                    {"Withdrawn Tickets", String.valueOf(sum.withdrawnTickets())},
                    {"Reopened Tickets", String.valueOf(sum.reopenedTickets())},
                    {"Overdue / SLA Breached", String.valueOf(sum.overdueTickets())},
                    {"SLA Compliance Rate", sum.slaComplianceRate() + "%"},
                    {"Average Resolution Time", sum.avgResolutionHours() + " hours"}
            };

            for (int i = 0; i < metrics.length; i++) {
                Row r = sheet.createRow(rowNum++);
                Cell cLabel = r.createCell(0);
                cLabel.setCellValue(metrics[i][0]);
                cLabel.setCellStyle(i % 2 == 0 ? s.zebraStyle : s.regularStyle);

                Cell cVal = r.createCell(1);
                cVal.setCellValue(metrics[i][1]);
                cVal.setCellStyle(i % 2 == 0 ? s.zebraStyle : s.regularStyle);
            }
        }
    }

    private void createStatusPrioritySheet(
            XSSFWorkbook wb, DashboardAnalyticsResponseDTO data, StylePalette s
    ) {
        Sheet sheet = wb.createSheet("Status & Priority");
        sheet.setDisplayGridlines(true);

        int rowNum = 0;

        // Status Table
        Row stHeader = sheet.createRow(rowNum++);
        stHeader.setHeightInPoints(22);
        Cell stHeaderCell = stHeader.createCell(0);
        stHeaderCell.setCellValue("TICKET STATUS BREAKDOWN");
        stHeaderCell.setCellStyle(s.sectionHeaderStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 2));

        Row stColHeader = sheet.createRow(rowNum++);
        stColHeader.createCell(0).setCellValue("Status");
        stColHeader.getCell(0).setCellStyle(s.headerStyle);
        stColHeader.createCell(1).setCellValue("Ticket Count");
        stColHeader.getCell(1).setCellStyle(s.headerStyle);
        stColHeader.createCell(2).setCellValue("Share (%)");
        stColHeader.getCell(2).setCellStyle(s.headerStyle);

        long stTotal = 0;
        if (data.statusDistribution() != null) {
            for (int i = 0; i < data.statusDistribution().size(); i++) {
                DistributionItemDTO item = data.statusDistribution().get(i);
                Row r = sheet.createRow(rowNum++);
                CellStyle rowStyle = i % 2 == 0 ? s.zebraStyle : s.regularStyle;

                Cell c0 = r.createCell(0);
                c0.setCellValue(item.label());
                c0.setCellStyle(rowStyle);

                Cell c1 = r.createCell(1);
                c1.setCellValue(item.count());
                c1.setCellStyle(rowStyle);

                Cell c2 = r.createCell(2);
                c2.setCellValue(item.percentage() + "%");
                c2.setCellStyle(rowStyle);

                stTotal += item.count();
            }
        }

        Row stTotalRow = sheet.createRow(rowNum++);
        stTotalRow.createCell(0).setCellValue("Total");
        stTotalRow.getCell(0).setCellStyle(s.totalStyle);
        stTotalRow.createCell(1).setCellValue(stTotal);
        stTotalRow.getCell(1).setCellStyle(s.totalStyle);
        stTotalRow.createCell(2).setCellValue("100.0%");
        stTotalRow.getCell(2).setCellStyle(s.totalStyle);

        rowNum += 3;

        // Priority Table
        Row prHeader = sheet.createRow(rowNum++);
        prHeader.setHeightInPoints(22);
        Cell prHeaderCell = prHeader.createCell(0);
        prHeaderCell.setCellValue("TICKET PRIORITY BREAKDOWN");
        prHeaderCell.setCellStyle(s.sectionHeaderStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 2));

        Row prColHeader = sheet.createRow(rowNum++);
        prColHeader.createCell(0).setCellValue("Priority Level");
        prColHeader.getCell(0).setCellStyle(s.headerStyle);
        prColHeader.createCell(1).setCellValue("Ticket Count");
        prColHeader.getCell(1).setCellStyle(s.headerStyle);
        prColHeader.createCell(2).setCellValue("Share (%)");
        prColHeader.getCell(2).setCellStyle(s.headerStyle);

        long prTotal = 0;
        if (data.priorityDistribution() != null) {
            for (int i = 0; i < data.priorityDistribution().size(); i++) {
                DistributionItemDTO item = data.priorityDistribution().get(i);
                Row r = sheet.createRow(rowNum++);
                CellStyle rowStyle = i % 2 == 0 ? s.zebraStyle : s.regularStyle;

                Cell c0 = r.createCell(0);
                c0.setCellValue(item.label());
                c0.setCellStyle(rowStyle);

                Cell c1 = r.createCell(1);
                c1.setCellValue(item.count());
                c1.setCellStyle(rowStyle);

                Cell c2 = r.createCell(2);
                c2.setCellValue(item.percentage() + "%");
                c2.setCellStyle(rowStyle);

                prTotal += item.count();
            }
        }

        Row prTotalRow = sheet.createRow(rowNum++);
        prTotalRow.createCell(0).setCellValue("Total");
        prTotalRow.getCell(0).setCellStyle(s.totalStyle);
        prTotalRow.createCell(1).setCellValue(prTotal);
        prTotalRow.getCell(1).setCellStyle(s.totalStyle);
        prTotalRow.createCell(2).setCellValue("100.0%");
        prTotalRow.getCell(2).setCellStyle(s.totalStyle);
    }

    private void createDepartmentCategorySheet(
            XSSFWorkbook wb, DashboardAnalyticsResponseDTO data, StylePalette s
    ) {
        Sheet sheet = wb.createSheet("Department & Category");
        sheet.setDisplayGridlines(true);

        int rowNum = 0;

        // Department Section
        Row deptHeader = sheet.createRow(rowNum++);
        deptHeader.setHeightInPoints(22);
        Cell dhCell = deptHeader.createCell(0);
        dhCell.setCellValue("DEPARTMENT PERFORMANCE");
        dhCell.setCellStyle(s.sectionHeaderStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 3));

        Row deptColHeader = sheet.createRow(rowNum++);
        String[] deptCols = {"Department Name", "Total Tickets", "Open Tickets", "Resolved Tickets"};
        for (int i = 0; i < deptCols.length; i++) {
            Cell c = deptColHeader.createCell(i);
            c.setCellValue(deptCols[i]);
            c.setCellStyle(s.headerStyle);
        }

        if (data.departmentDistribution() != null && !data.departmentDistribution().isEmpty()) {
            for (int i = 0; i < data.departmentDistribution().size(); i++) {
                DepartmentMetricDTO d = data.departmentDistribution().get(i);
                Row r = sheet.createRow(rowNum++);
                CellStyle rowStyle = i % 2 == 0 ? s.zebraStyle : s.regularStyle;

                Cell c0 = r.createCell(0);
                c0.setCellValue(d.departmentName());
                c0.setCellStyle(rowStyle);

                Cell c1 = r.createCell(1);
                c1.setCellValue(d.totalTickets());
                c1.setCellStyle(rowStyle);

                Cell c2 = r.createCell(2);
                c2.setCellValue(d.openTickets());
                c2.setCellStyle(rowStyle);

                Cell c3 = r.createCell(3);
                c3.setCellValue(d.resolvedTickets());
                c3.setCellStyle(rowStyle);
            }
        } else {
            Row r = sheet.createRow(rowNum++);
            r.createCell(0).setCellValue("No department records available");
            r.getCell(0).setCellStyle(s.regularStyle);
        }

        rowNum += 3;

        // Category Section
        Row catHeader = sheet.createRow(rowNum++);
        catHeader.setHeightInPoints(22);
        Cell chCell = catHeader.createCell(0);
        chCell.setCellValue("CATEGORY DISTRIBUTION");
        chCell.setCellStyle(s.sectionHeaderStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 2));

        Row catColHeader = sheet.createRow(rowNum++);
        catColHeader.createCell(0).setCellValue("Category Name");
        catColHeader.getCell(0).setCellStyle(s.headerStyle);
        catColHeader.createCell(1).setCellValue("Ticket Count");
        catColHeader.getCell(1).setCellStyle(s.headerStyle);
        catColHeader.createCell(2).setCellValue("Share (%)");
        catColHeader.getCell(2).setCellStyle(s.headerStyle);

        if (data.categoryDistribution() != null && !data.categoryDistribution().isEmpty()) {
            for (int i = 0; i < data.categoryDistribution().size(); i++) {
                DistributionItemDTO item = data.categoryDistribution().get(i);
                Row r = sheet.createRow(rowNum++);
                CellStyle rowStyle = i % 2 == 0 ? s.zebraStyle : s.regularStyle;

                Cell c0 = r.createCell(0);
                c0.setCellValue(item.label());
                c0.setCellStyle(rowStyle);

                Cell c1 = r.createCell(1);
                c1.setCellValue(item.count());
                c1.setCellStyle(rowStyle);

                Cell c2 = r.createCell(2);
                c2.setCellValue(item.percentage() + "%");
                c2.setCellStyle(rowStyle);
            }
        } else {
            Row r = sheet.createRow(rowNum++);
            r.createCell(0).setCellValue("No category records available");
            r.getCell(0).setCellStyle(s.regularStyle);
        }
    }

    private void createAgentWorkloadSheet(
            XSSFWorkbook wb, DashboardAnalyticsResponseDTO data, StylePalette s
    ) {
        Sheet sheet = wb.createSheet("Agent Workload");
        sheet.setDisplayGridlines(true);

        int rowNum = 0;

        Row headerRow = sheet.createRow(rowNum++);
        headerRow.setHeightInPoints(22);
        Cell hCell = headerRow.createCell(0);
        hCell.setCellValue("SUPPORT AGENT WORKLOAD & ASSIGNMENTS");
        hCell.setCellStyle(s.sectionHeaderStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 4));

        Row colRow = sheet.createRow(rowNum++);
        String[] cols = {"Agent Name", "Email Address", "Department", "Active Tickets", "Total Assigned"};
        for (int i = 0; i < cols.length; i++) {
            Cell c = colRow.createCell(i);
            c.setCellValue(cols[i]);
            c.setCellStyle(s.headerStyle);
        }

        for (int i = 0; i < data.agentWorkload().size(); i++) {
            AgentWorkloadDTO ag = data.agentWorkload().get(i);
            Row r = sheet.createRow(rowNum++);
            CellStyle rowStyle = i % 2 == 0 ? s.zebraStyle : s.regularStyle;

            Cell c0 = r.createCell(0);
            c0.setCellValue(ag.agentName());
            c0.setCellStyle(rowStyle);

            Cell c1 = r.createCell(1);
            c1.setCellValue(ag.email());
            c1.setCellStyle(rowStyle);

            Cell c2 = r.createCell(2);
            c2.setCellValue(ag.departmentName());
            c2.setCellStyle(rowStyle);

            Cell c3 = r.createCell(3);
            c3.setCellValue(ag.activeTickets());
            c3.setCellStyle(rowStyle);

            Cell c4 = r.createCell(4);
            c4.setCellValue(ag.totalTickets());
            c4.setCellStyle(rowStyle);
        }
    }

    private void createTicketRecordsSheet(
            XSSFWorkbook wb, List<ExportTicketDetailDTO> tickets, StylePalette s, DateTimeFormatter dtf
    ) {
        Sheet sheet = wb.createSheet("Ticket Records");
        sheet.setDisplayGridlines(true);

        int rowNum = 0;

        Row colRow = sheet.createRow(rowNum++);
        colRow.setHeightInPoints(24);
        String[] headers = {
                "Ticket #", "Subject", "Department", "Category", "Subcategory",
                "Status", "Priority", "SLA Status", "Requester", "Requester Email",
                "Assigned Agent", "Created At", "Resolved At"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell c = colRow.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(s.headerStyle);
        }

        if (tickets != null && !tickets.isEmpty()) {
            for (int i = 0; i < tickets.size(); i++) {
                ExportTicketDetailDTO t = tickets.get(i);
                Row r = sheet.createRow(rowNum++);
                CellStyle rowStyle = i % 2 == 0 ? s.zebraStyle : s.regularStyle;

                r.createCell(0).setCellValue(t.ticketNumber());
                r.getCell(0).setCellStyle(rowStyle);

                r.createCell(1).setCellValue(t.subject());
                r.getCell(1).setCellStyle(rowStyle);

                r.createCell(2).setCellValue(t.departmentName() != null ? t.departmentName() : "");
                r.getCell(2).setCellStyle(rowStyle);

                r.createCell(3).setCellValue(t.categoryName() != null ? t.categoryName() : "");
                r.getCell(3).setCellStyle(rowStyle);

                r.createCell(4).setCellValue(t.subCategoryName() != null ? t.subCategoryName() : "");
                r.getCell(4).setCellStyle(rowStyle);

                r.createCell(5).setCellValue(t.status() != null ? t.status() : "");
                r.getCell(5).setCellStyle(rowStyle);

                r.createCell(6).setCellValue(t.priority() != null ? t.priority() : "");
                r.getCell(6).setCellStyle(rowStyle);

                r.createCell(7).setCellValue(t.slaStatus() != null ? t.slaStatus() : "N/A");
                r.getCell(7).setCellStyle(rowStyle);

                r.createCell(8).setCellValue(t.requesterName() != null ? t.requesterName() : "");
                r.getCell(8).setCellStyle(rowStyle);

                r.createCell(9).setCellValue(t.requesterEmail() != null ? t.requesterEmail() : "");
                r.getCell(9).setCellStyle(rowStyle);

                r.createCell(10).setCellValue(t.assignedAgentName() != null ? t.assignedAgentName() : "Unassigned");
                r.getCell(10).setCellStyle(rowStyle);

                r.createCell(11).setCellValue(t.createdAt() != null ? dtf.format(t.createdAt()) : "");
                r.getCell(11).setCellStyle(rowStyle);

                r.createCell(12).setCellValue(t.resolvedAt() != null ? dtf.format(t.resolvedAt()) : "");
                r.getCell(12).setCellStyle(rowStyle);
            }
        } else {
            Row emptyRow = sheet.createRow(rowNum++);
            Cell c = emptyRow.createCell(0);
            c.setCellValue("No tickets match the selected filters");
            c.setCellStyle(s.regularStyle);
        }
    }

    private void createTicketOverviewSheet(
            XSSFWorkbook wb, DashboardAnalyticsResponseDTO data, StylePalette s, DateTimeFormatter dtf
    ) {
        Sheet sheet = wb.createSheet("Ticket Overview");
        sheet.setDisplayGridlines(true);

        int rowNum = 0;
        Row titleRow = sheet.createRow(rowNum++);
        titleRow.setHeightInPoints(34);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("EMPLOYEE HELP DESK - TICKET SUMMARY REPORT");
        titleCell.setCellStyle(s.titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

        rowNum++;
        Row infoRow = sheet.createRow(rowNum++);
        infoRow.createCell(0).setCellValue("Generated At:");
        infoRow.getCell(0).setCellStyle(s.boldLabelStyle);
        infoRow.createCell(1).setCellValue(dtf.format(Instant.now()));
        infoRow.getCell(1).setCellStyle(s.regularStyle);

        DashboardSummaryDTO sm = data != null ? data.summary() : null;
        Object[][] metrics = {
                {"Total Tickets Logged", sm != null ? sm.totalTickets() : 0L},
                {"Open Tickets", sm != null ? sm.openTickets() : 0L},
                {"In-Progress Tickets", sm != null ? sm.inProgressTickets() : 0L},
                {"Resolved Tickets", sm != null ? sm.resolvedTickets() : 0L},
                {"On Hold Tickets", sm != null ? sm.onHoldTickets() : 0L},
                {"Average Resolution Hours", (sm != null ? sm.avgResolutionHours() : 0.0) + " hrs"}
        };
        for (Object[] m : metrics) {
            Row r = sheet.createRow(rowNum++);
            Cell c0 = r.createCell(0);
            c0.setCellValue((String) m[0]);
            c0.setCellStyle(s.boldLabelStyle);
            Cell c1 = r.createCell(1);
            c1.setCellValue(String.valueOf(m[1]));
            c1.setCellStyle(s.regularStyle);
        }

        if (data != null && data.categoryDistribution() != null && !data.categoryDistribution().isEmpty()) {
            rowNum += 2;
            Row catHeader = sheet.createRow(rowNum++);
            catHeader.setHeightInPoints(22);
            Cell chCell = catHeader.createCell(0);
            chCell.setCellValue("CATEGORY BREAKDOWN");
            chCell.setCellStyle(s.sectionHeaderStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 2));

            Row colH = sheet.createRow(rowNum++);
            colH.createCell(0).setCellValue("Category Name");
            colH.getCell(0).setCellStyle(s.headerStyle);
            colH.createCell(1).setCellValue("Ticket Count");
            colH.getCell(1).setCellStyle(s.headerStyle);
            colH.createCell(2).setCellValue("Share (%)");
            colH.getCell(2).setCellStyle(s.headerStyle);

            for (int i = 0; i < data.categoryDistribution().size(); i++) {
                DistributionItemDTO item = data.categoryDistribution().get(i);
                Row r = sheet.createRow(rowNum++);
                CellStyle rowStyle = i % 2 == 0 ? s.zebraStyle : s.regularStyle;
                Cell c0 = r.createCell(0);
                c0.setCellValue(item.label());
                c0.setCellStyle(rowStyle);
                Cell c1 = r.createCell(1);
                c1.setCellValue(item.count());
                c1.setCellStyle(rowStyle);
                Cell c2 = r.createCell(2);
                c2.setCellValue(item.percentage() + "%");
                c2.setCellStyle(rowStyle);
            }
        }
    }

    private void createUserOverviewSheet(
            XSSFWorkbook wb, DashboardAnalyticsResponseDTO data, StylePalette s, DateTimeFormatter dtf
    ) {
        Sheet sheet = wb.createSheet("User Overview");
        sheet.setDisplayGridlines(true);

        int rowNum = 0;
        Row titleRow = sheet.createRow(rowNum++);
        titleRow.setHeightInPoints(34);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("EMPLOYEE HELP DESK - USER & WORKLOAD REPORT");
        titleCell.setCellStyle(s.titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));

        rowNum++;
        Row infoRow = sheet.createRow(rowNum++);
        infoRow.createCell(0).setCellValue("Generated At:");
        infoRow.getCell(0).setCellStyle(s.boldLabelStyle);
        infoRow.createCell(1).setCellValue(dtf.format(Instant.now()));
        infoRow.getCell(1).setCellStyle(s.regularStyle);

        DashboardSummaryDTO sm = data != null ? data.summary() : null;
        Object[][] userMetrics = {
                {"Authorized Role", data != null ? data.role() : "N/A"},
                {"Department Name", data != null && data.departmentName() != null ? data.departmentName() : "Organization-Wide"},
                {"Total Tickets In Scope", sm != null ? sm.totalTickets() : 0L},
                {"Assigned Tickets", sm != null ? sm.assignedTickets() : 0L},
                {"Unassigned Tickets", sm != null ? sm.unassignedTickets() : 0L}
        };
        for (Object[] m : userMetrics) {
            Row r = sheet.createRow(rowNum++);
            Cell c0 = r.createCell(0);
            c0.setCellValue((String) m[0]);
            c0.setCellStyle(s.boldLabelStyle);
            Cell c1 = r.createCell(1);
            c1.setCellValue(String.valueOf(m[1]));
            c1.setCellStyle(s.regularStyle);
        }
    }

    private void createSlaSummarySheet(
            XSSFWorkbook wb, DashboardAnalyticsResponseDTO data, StylePalette s, DateTimeFormatter dtf
    ) {
        Sheet sheet = wb.createSheet("SLA Performance");
        sheet.setDisplayGridlines(true);

        int rowNum = 0;
        Row titleRow = sheet.createRow(rowNum++);
        titleRow.setHeightInPoints(34);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("EMPLOYEE HELP DESK - SLA PERFORMANCE SUMMARY");
        titleCell.setCellStyle(s.titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));

        rowNum++;
        DashboardSummaryDTO sm = data != null ? data.summary() : null;
        Object[][] slaKpis = {
                {"Total Tickets Tracked", sm != null ? sm.totalTickets() : 0L},
                {"SLA Compliant Tickets", sm != null ? sm.slaCompliantTickets() : 0L},
                {"SLA Breached Tickets", sm != null ? sm.slaBreachedTickets() : 0L},
                {"SLA Warning Tickets", sm != null ? sm.slaWarningTickets() : 0L},
                {"SLA Compliance Rate (%)", (sm != null ? sm.slaComplianceRate() : 100.0) + "%"},
                {"Average Resolution Time", (sm != null ? sm.avgResolutionHours() : 0.0) + " hrs"}
        };
        for (Object[] kpi : slaKpis) {
            Row r = sheet.createRow(rowNum++);
            Cell c0 = r.createCell(0);
            c0.setCellValue((String) kpi[0]);
            c0.setCellStyle(s.boldLabelStyle);
            Cell c1 = r.createCell(1);
            c1.setCellValue(String.valueOf(kpi[1]));
            c1.setCellStyle(s.regularStyle);
        }
    }

    private void createSlaTicketRecordsSheet(
            XSSFWorkbook wb, List<ExportTicketDetailDTO> tickets, StylePalette s, DateTimeFormatter dtf
    ) {
        Sheet sheet = wb.createSheet("SLA Ticket Records");
        sheet.setDisplayGridlines(true);

        int rowNum = 0;
        Row colRow = sheet.createRow(rowNum++);
        colRow.setHeightInPoints(24);
        String[] headers = {
                "Ticket #", "Subject", "Department", "Priority", "Status",
                "SLA Status", "Target Deadline", "Resolved At", "Requester", "Assigned Agent"
        };
        for (int i = 0; i < headers.length; i++) {
            Cell c = colRow.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(s.headerStyle);
        }

        if (tickets != null && !tickets.isEmpty()) {
            for (int i = 0; i < tickets.size(); i++) {
                ExportTicketDetailDTO t = tickets.get(i);
                Row r = sheet.createRow(rowNum++);
                CellStyle rowStyle = i % 2 == 0 ? s.zebraStyle : s.regularStyle;

                r.createCell(0).setCellValue(t.ticketNumber() != null ? t.ticketNumber() : "#" + t.id());
                r.getCell(0).setCellStyle(rowStyle);

                r.createCell(1).setCellValue(t.subject() != null ? t.subject() : "");
                r.getCell(1).setCellStyle(rowStyle);

                r.createCell(2).setCellValue(t.departmentName() != null ? t.departmentName() : "-");
                r.getCell(2).setCellStyle(rowStyle);

                r.createCell(3).setCellValue(t.priority() != null ? t.priority() : "-");
                r.getCell(3).setCellStyle(rowStyle);

                r.createCell(4).setCellValue(t.status() != null ? t.status() : "-");
                r.getCell(4).setCellStyle(rowStyle);

                r.createCell(5).setCellValue(t.slaStatus() != null ? t.slaStatus() : "COMPLIANT");
                r.getCell(5).setCellStyle(rowStyle);

                r.createCell(6).setCellValue(t.deadlineAt() != null ? dtf.format(t.deadlineAt()) : "-");
                r.getCell(6).setCellStyle(rowStyle);

                r.createCell(7).setCellValue(t.resolvedAt() != null ? dtf.format(t.resolvedAt()) : "-");
                r.getCell(7).setCellStyle(rowStyle);

                r.createCell(8).setCellValue(t.requesterName() != null ? t.requesterName() : "-");
                r.getCell(8).setCellStyle(rowStyle);

                r.createCell(9).setCellValue(t.assignedAgentName() != null ? t.assignedAgentName() : "Unassigned");
                r.getCell(9).setCellStyle(rowStyle);
            }
        } else {
            Row r = sheet.createRow(rowNum++);
            r.createCell(0).setCellValue("No SLA ticket records available");
            r.getCell(0).setCellStyle(s.regularStyle);
        }
    }

    private record StylePalette(
            XSSFCellStyle titleStyle,
            XSSFCellStyle sectionHeaderStyle,
            XSSFCellStyle headerStyle,
            XSSFCellStyle boldLabelStyle,
            XSSFCellStyle regularStyle,
            XSSFCellStyle zebraStyle,
            XSSFCellStyle totalStyle
    ) {}

    private StylePalette createStylePalette(XSSFWorkbook wb) {
        // Fonts
        XSSFFont titleFont = wb.createFont();
        titleFont.setFontName("Calibri");
        titleFont.setFontHeightInPoints((short) 14);
        titleFont.setBold(true);
        titleFont.setColor(new XSSFColor(new byte[]{(byte) 255, (byte) 255, (byte) 255}, null));

        XSSFFont headerFont = wb.createFont();
        headerFont.setFontName("Calibri");
        headerFont.setFontHeightInPoints((short) 10);
        headerFont.setBold(true);
        headerFont.setColor(new XSSFColor(new byte[]{(byte) 255, (byte) 255, (byte) 255}, null));

        XSSFFont boldFont = wb.createFont();
        boldFont.setFontName("Calibri");
        boldFont.setFontHeightInPoints((short) 10);
        boldFont.setBold(true);

        XSSFFont regularFont = wb.createFont();
        regularFont.setFontName("Calibri");
        regularFont.setFontHeightInPoints((short) 10);

        // Title Style
        XSSFCellStyle titleStyle = wb.createCellStyle();
        titleStyle.setFont(titleFont);
        titleStyle.setFillForegroundColor(new XSSFColor(COLOR_PRIMARY_NAVY, null));
        titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        titleStyle.setAlignment(HorizontalAlignment.LEFT);
        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // Section Header Style
        XSSFCellStyle sectionStyle = wb.createCellStyle();
        sectionStyle.setFont(boldFont);
        sectionStyle.setFillForegroundColor(new XSSFColor(COLOR_SECTION_BG, null));
        sectionStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        sectionStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // Column Header Style
        XSSFCellStyle headerStyle = wb.createCellStyle();
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(new XSSFColor(COLOR_PRIMARY_NAVY, null));
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.LEFT);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(headerStyle, BorderStyle.THIN);

        // Bold Label Style
        XSSFCellStyle boldLabelStyle = wb.createCellStyle();
        boldLabelStyle.setFont(boldFont);

        // Regular Cell Style
        XSSFCellStyle regularStyle = wb.createCellStyle();
        regularStyle.setFont(regularFont);
        setBorders(regularStyle, BorderStyle.THIN);

        // Zebra Row Style
        XSSFCellStyle zebraStyle = wb.createCellStyle();
        zebraStyle.setFont(regularFont);
        zebraStyle.setFillForegroundColor(new XSSFColor(COLOR_ZEBRA_ROW, null));
        zebraStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setBorders(zebraStyle, BorderStyle.THIN);

        // Total Row Style
        XSSFCellStyle totalStyle = wb.createCellStyle();
        totalStyle.setFont(boldFont);
        totalStyle.setFillForegroundColor(new XSSFColor(COLOR_TOTAL_ROW, null));
        totalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        totalStyle.setBorderTop(BorderStyle.THIN);
        totalStyle.setBorderBottom(BorderStyle.DOUBLE);

        return new StylePalette(
                titleStyle, sectionStyle, headerStyle, boldLabelStyle, regularStyle, zebraStyle, totalStyle
        );
    }

    private void setBorders(CellStyle cs, BorderStyle bs) {
        cs.setBorderTop(bs);
        cs.setBorderBottom(bs);
        cs.setBorderLeft(bs);
        cs.setBorderRight(bs);
    }
}
