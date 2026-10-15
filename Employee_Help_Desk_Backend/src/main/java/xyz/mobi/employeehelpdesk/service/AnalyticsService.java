package xyz.mobi.employeehelpdesk.service;

import xyz.mobi.employeehelpdesk.dto.analytics.DashboardAnalyticsResponseDTO;
import xyz.mobi.employeehelpdesk.dto.analytics.ExportTicketDetailDTO;

import java.time.LocalDate;
import java.util.List;

public interface AnalyticsService {

    DashboardAnalyticsResponseDTO getDashboardAnalytics(LocalDate fromDate, LocalDate toDate, Long departmentId);

    List<ExportTicketDetailDTO> getExportTicketDetails(LocalDate fromDate, LocalDate toDate, Long departmentId);
}
