package xyz.mobi.employeehelpdesk.service;

import org.springframework.http.ResponseEntity;
import xyz.mobi.employeehelpdesk.dto.report.ReportSummaryRequestDTO;
import xyz.mobi.employeehelpdesk.dto.report.ReportSummaryResponseDTO;

public interface ReportIntegrationService {

    ReportSummaryResponseDTO getSummaryReportJson(ReportSummaryRequestDTO request, String rawAuthHeader);

    ResponseEntity<byte[]> getSummaryReportExcel(ReportSummaryRequestDTO request, String rawAuthHeader);

    ResponseEntity<?> getSummaryReport(ReportSummaryRequestDTO request, String rawAuthHeader);
}
