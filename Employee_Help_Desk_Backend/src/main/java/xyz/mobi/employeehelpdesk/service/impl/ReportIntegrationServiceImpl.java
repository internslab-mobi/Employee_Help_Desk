package xyz.mobi.employeehelpdesk.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.mobi.employeehelpdesk.client.ReportServiceClient;
import xyz.mobi.employeehelpdesk.dto.analytics.DashboardAnalyticsResponseDTO;
import xyz.mobi.employeehelpdesk.dto.analytics.ExportTicketDetailDTO;
import xyz.mobi.employeehelpdesk.dto.report.*;
import xyz.mobi.employeehelpdesk.entity.Employee;
import xyz.mobi.employeehelpdesk.entity.enums.UserRole;
import xyz.mobi.employeehelpdesk.exception.ResourceNotFoundException;
import xyz.mobi.employeehelpdesk.exception.UnauthorizedException;
import xyz.mobi.employeehelpdesk.repository.EmployeeRepository;
import xyz.mobi.employeehelpdesk.service.AnalyticsService;
import xyz.mobi.employeehelpdesk.service.AuthService;
import xyz.mobi.employeehelpdesk.service.ReportIntegrationService;

import xyz.mobi.employeehelpdesk.service.helperservice.EmailService;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportIntegrationServiceImpl implements ReportIntegrationService {

    private final AuthService authService;
    private final EmployeeRepository employeeRepository;
    private final AnalyticsService analyticsService;
    private final ReportServiceClient reportServiceClient;
    private final EmailService emailService;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getSummaryReport(ReportSummaryRequestDTO request, String rawAuthHeader) {
        ReportFormat format = request.getFormat() != null ? request.getFormat() : ReportFormat.JSON;
        if (format == ReportFormat.EXCEL) {
            return getSummaryReportExcel(request, rawAuthHeader);
        } else {
            ReportSummaryResponseDTO jsonResponse = getSummaryReportJson(request, rawAuthHeader);
            return ResponseEntity.ok(jsonResponse);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ReportSummaryResponseDTO getSummaryReportJson(ReportSummaryRequestDTO request, String rawAuthHeader) {
        ReportGenerationPayloadDTO payload = prepareAuthorizedPayload(request, ReportFormat.JSON);
        return reportServiceClient.generateJsonSummary(payload, rawAuthHeader);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> getSummaryReportExcel(ReportSummaryRequestDTO request, String rawAuthHeader) {
        ReportGenerationPayloadDTO payload = prepareAuthorizedPayload(request, ReportFormat.EXCEL);
        ResponseEntity<byte[]> response = reportServiceClient.generateExcelReport(payload, rawAuthHeader);

        byte[] excelBytes = response.getBody();

        if (excelBytes != null && excelBytes.length > 0 && payload.requesterEmail() != null) {
            String filename = resolveFilename(response, payload);
            String reportName = formatReportTypeName(payload.reportType());
            String subject = "Exported Help Desk Report - " + reportName;
            String body = "Dear " + (payload.requesterName() != null ? payload.requesterName() : "Employee") + ",\n\n"
                    + "Your requested " + reportName + " Excel report has been successfully generated.\n"
                    + "Please find the report attached to this email.\n\n"
                    + "Report Details:\n"
                    + "- Report Category: " + reportName + "\n"
                    + "- Requested By: " + payload.requesterName() + " (" + payload.requestedByRole() + ")\n"
                    + "- Date Generated: " + LocalDate.now() + "\n\n"
                    + "Best regards,\n"
                    + "Employee Help Desk System";

            log.info("Dispatching exported Excel report email to registered user: {}", payload.requesterEmail());
            emailService.sendReportEmailWithAttachment(
                    payload.requesterEmail(),
                    payload.requesterName(),
                    subject,
                    body,
                    filename,
                    excelBytes
            );
        }

        return response;
    }

    private String resolveFilename(ResponseEntity<byte[]> response, ReportGenerationPayloadDTO payload) {
        try {
            if (response.getHeaders().getContentDisposition() != null
                    && response.getHeaders().getContentDisposition().getFilename() != null) {
                return response.getHeaders().getContentDisposition().getFilename();
            }
        } catch (Exception ignored) {
        }
        String todayStr = LocalDate.now().toString();
        if (payload.reportType() == ReportType.AGENT_SUMMARY) {
            return "agent-assigned-tickets-summary-" + todayStr + ".xlsx";
        }
        String rolePrefix = payload.requestedByRole() != null ? payload.requestedByRole() : "Report";
        String typePrefix = payload.reportType() != null ? payload.reportType().name() : "Summary";
        return rolePrefix + "_" + typePrefix + "_" + todayStr + ".xlsx";
    }

    private String formatReportTypeName(ReportType type) {
        return type != null ? type.name().replace('_', ' ') : "Summary";
    }

    /**
     * Authenticates user, verifies RBAC, scopes data, queries live database via AnalyticsService,
     * applies permitted filters, and packs into the payload for the independent report microservice.
     */
    private ReportGenerationPayloadDTO prepareAuthorizedPayload(ReportSummaryRequestDTO request, ReportFormat format) {
        Long currentEmployeeId = authService.getCurrentEmployeeId();
        Employee employee = employeeRepository.findById(currentEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated employee not found: " + currentEmployeeId));

        UserRole role = employee.getRole() != null ? employee.getRole() : UserRole.EMPLOYEE;
        ReportType reportType = request.getReportType() != null ? request.getReportType() : ReportType.GENERAL_SUMMARY;

        // 1. Enforce strict server-side RBAC report category authorization
        enforceReportAuthorization(role, reportType);

        // 2. Resolve department scope based on role permissions
        Long effectiveDepartmentId = resolveEffectiveDepartmentId(role, employee, request.getDepartmentId());

        // 3. Query real persisted application database metrics via AnalyticsService
        DashboardAnalyticsResponseDTO analytics = analyticsService.getDashboardAnalytics(
                request.getFromDate(), request.getToDate(), effectiveDepartmentId
        );

        List<ExportTicketDetailDTO> tickets = analyticsService.getExportTicketDetails(
                request.getFromDate(), request.getToDate(), effectiveDepartmentId
        );

        // 4. Apply permitted filters (status, priority, search) if specified in request
        if (request.getStatus() != null && tickets != null) {
            tickets = tickets.stream()
                    .filter(t -> t.status() == request.getStatus())
                    .toList();
        }
        if (request.getPriority() != null && tickets != null) {
            tickets = tickets.stream()
                    .filter(t -> t.priority() == request.getPriority())
                    .toList();
        }
        if (request.getSearch() != null && !request.getSearch().isBlank() && tickets != null) {
            String term = request.getSearch().trim().toLowerCase();
            tickets = tickets.stream()
                    .filter(t -> (t.ticketNumber() != null && t.ticketNumber().toLowerCase().contains(term))
                            || (t.subject() != null && t.subject().toLowerCase().contains(term))
                            || (t.requesterName() != null && t.requesterName().toLowerCase().contains(term))
                            || (t.departmentName() != null && t.departmentName().toLowerCase().contains(term))
                            || (t.categoryName() != null && t.categoryName().toLowerCase().contains(term))
                            || (t.subCategoryName() != null && t.subCategoryName().toLowerCase().contains(term)))
                    .toList();
        }

        if (format == ReportFormat.EXCEL && reportType == ReportType.AGENT_SUMMARY && (tickets == null || tickets.isEmpty())) {
            throw new ResourceNotFoundException("No assigned tickets match the selected criteria to export.");
        }

        String requesterFullName = (employee.getFirstName() + " " +
                (employee.getLastName() != null ? employee.getLastName() : "")).trim();

        log.info("Constructing report payload: role={}, employeeId={}, type={}, format={}, records={}",
                role, employee.getId(), reportType, format, tickets != null ? tickets.size() : 0);

        return ReportGenerationPayloadDTO.builder()
                .reportType(reportType)
                .format(format)
                .requestedByRole(role.name())
                .requesterEmployeeId(employee.getId())
                .requesterEmail(employee.getEmail())
                .requesterName(requesterFullName)
                .requesterTimezone(employee.getTimezone() != null ? employee.getTimezone() : "UTC")
                .fromDate(request.getFromDate())
                .toDate(request.getToDate())
                .departmentId(effectiveDepartmentId)
                .status(request.getStatus() != null ? request.getStatus().name() : null)
                .priority(request.getPriority() != null ? request.getPriority().name() : null)
                .analytics(analytics)
                .tickets(tickets != null ? tickets : Collections.emptyList())
                .additionalFilters(Collections.emptyMap())
                .build();
    }

    /**
     * RBAC Policy:
     * - EMPLOYEE: Only permitted summaries of own tickets (GENERAL_SUMMARY, TICKET_SUMMARY, USER_SUMMARY, SLA_SUMMARY).
     * - AGENT: Permitted summaries of assigned tickets and workload (GENERAL_SUMMARY, TICKET_SUMMARY, USER_SUMMARY, AGENT_SUMMARY, SLA_SUMMARY).
     * - MANAGER: Department-level summaries (GENERAL_SUMMARY, TICKET_SUMMARY, USER_SUMMARY, AGENT_SUMMARY, MANAGER_SUMMARY, SLA_SUMMARY).
     * - ADMIN: Organization-wide access to all summary reports.
     */
    private void enforceReportAuthorization(UserRole role, ReportType reportType) {
        switch (role) {
            case EMPLOYEE -> {
                if (reportType == ReportType.ADMIN_SUMMARY ||
                    reportType == ReportType.MANAGER_SUMMARY ||
                    reportType == ReportType.EMPLOYEE_SUMMARY ||
                    reportType == ReportType.AGENT_SUMMARY) {
                    throw new UnauthorizedException("Employees are not authorized to access " + reportType + " reports.");
                }
            }
            case AGENT -> {
                if (reportType == ReportType.ADMIN_SUMMARY ||
                    reportType == ReportType.MANAGER_SUMMARY ||
                    reportType == ReportType.EMPLOYEE_SUMMARY) {
                    throw new UnauthorizedException("Agents are not authorized to access " + reportType + " reports.");
                }
            }
            case MANAGER -> {
                if (reportType == ReportType.ADMIN_SUMMARY ||
                    reportType == ReportType.EMPLOYEE_SUMMARY) {
                    throw new UnauthorizedException("Managers are not authorized to access organization-wide " + reportType + " reports.");
                }
            }
            case ADMIN -> {
                // Administrators are authorized to view all report categories
            }
        }
    }

    /**
     * Department isolation:
     * - MANAGER: locked strictly to their department.
     * - ADMIN: can view specific department or org-wide (null).
     * - EMPLOYEE / AGENT: department filter is null/managed by personal ticket scope.
     */
    private Long resolveEffectiveDepartmentId(UserRole role, Employee employee, Long requestedDeptId) {
        switch (role) {
            case MANAGER -> {
                if (employee.getDepartment() == null) {
                    throw new UnauthorizedException("Manager is not assigned to any department.");
                }
                Long managerDeptId = employee.getDepartment().getId();
                if (requestedDeptId != null && !requestedDeptId.equals(managerDeptId)) {
                    throw new UnauthorizedException("Managers are not authorized to access reports for another department.");
                }
                return managerDeptId;
            }
            case ADMIN -> {
                return requestedDeptId;
            }
            default -> {
                return null;
            }
        }
    }
}
