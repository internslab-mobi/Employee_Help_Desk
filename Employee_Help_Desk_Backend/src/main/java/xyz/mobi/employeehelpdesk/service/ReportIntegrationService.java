package xyz.mobi.employeehelpdesk.service;

import org.springframework.http.ResponseEntity;
import xyz.mobi.employeehelpdesk.dto.report.ReportSummaryRequestDTO;
import xyz.mobi.employeehelpdesk.dto.report.ReportSummaryResponseDTO;

import xyz.mobi.employeehelpdesk.security.UserPrincipal;

public interface ReportIntegrationService {

    ReportSummaryResponseDTO getSummaryReportJson(ReportSummaryRequestDTO request, String rawAuthHeader, UserPrincipal userPrincipal);

    ResponseEntity<byte[]> getSummaryReportExcel(ReportSummaryRequestDTO request, String rawAuthHeader, UserPrincipal userPrincipal);

    ResponseEntity<?> getSummaryReport(ReportSummaryRequestDTO request, String rawAuthHeader, UserPrincipal userPrincipal);
}
