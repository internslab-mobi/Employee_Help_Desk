package xyz.mobi.employeehelpdesk.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import xyz.mobi.employeehelpdesk.client.ReportServiceClient;
import xyz.mobi.employeehelpdesk.dto.analytics.DashboardAnalyticsResponseDTO;
import xyz.mobi.employeehelpdesk.dto.analytics.ExportTicketDetailDTO;
import xyz.mobi.employeehelpdesk.dto.report.*;
import xyz.mobi.employeehelpdesk.entity.Employee;
import xyz.mobi.employeehelpdesk.entity.enums.Priority;
import xyz.mobi.employeehelpdesk.entity.enums.TicketStatus;
import xyz.mobi.employeehelpdesk.entity.enums.UserRole;
import xyz.mobi.employeehelpdesk.client.NotificationClient;
import xyz.mobi.employeehelpdesk.repository.EmployeeRepository;
import xyz.mobi.employeehelpdesk.security.UserPrincipal;
import xyz.mobi.employeehelpdesk.service.impl.ReportIntegrationServiceImpl;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportIntegrationServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private AnalyticsService analyticsService;
    @Mock
    private ReportServiceClient reportServiceClient;
    @Mock
    private NotificationClient notificationClient;

    private ReportIntegrationServiceImpl reportIntegrationService;

    private Employee mockEmployee;
    private UserPrincipal adminPrincipal;
    private UserPrincipal agentPrincipal;

    @BeforeEach
    void setUp() {
        reportIntegrationService = new ReportIntegrationServiceImpl(
                employeeRepository,
                analyticsService,
                reportServiceClient,
                notificationClient
        );

        adminPrincipal = new UserPrincipal(1L, "admin@company.com", "", UserRole.ADMIN, "Asia/Kolkata", true, Collections.emptyList());
        agentPrincipal = new UserPrincipal(2L, "agent@company.com", "", UserRole.AGENT, "Asia/Kolkata", true, Collections.emptyList());

        mockEmployee = new Employee();
        mockEmployee.setId(1L);
        mockEmployee.setEmail("admin@company.com");
        mockEmployee.setFirstName("Admin");
        mockEmployee.setLastName("User");
        mockEmployee.setRole(UserRole.ADMIN);
        mockEmployee.setTimezone("Asia/Kolkata");
    }

    @Test
    @DisplayName("Should forward JSON summary report request to downstream ReportServiceClient")
    void shouldForwardJsonSummaryRequest() {
        ReportSummaryRequestDTO request = new ReportSummaryRequestDTO();
        request.setReportType(ReportType.TICKET_SUMMARY);
        request.setFormat(ReportFormat.JSON);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(mockEmployee));

        DashboardAnalyticsResponseDTO mockAnalytics = DashboardAnalyticsResponseDTO.builder()
                .role(UserRole.ADMIN)
                .employeeId(1L)
                .build();
        when(analyticsService.getDashboardAnalytics(any(), any(), any())).thenReturn(mockAnalytics);

        ReportSummaryResponseDTO mockResponse = ReportSummaryResponseDTO.builder()
                .reportType(ReportType.TICKET_SUMMARY)
                .format(ReportFormat.JSON)
                .totalRecords(10L)
                .build();
        when(reportServiceClient.generateJsonSummary(any(ReportGenerationPayloadDTO.class), anyString()))
                .thenReturn(mockResponse);

        ReportSummaryResponseDTO result = reportIntegrationService.getSummaryReportJson(request, "Bearer test-token", adminPrincipal);

        assertNotNull(result);
        assertEquals(ReportType.TICKET_SUMMARY, result.getReportType());
        assertEquals(10L, result.getTotalRecords());
        verify(reportServiceClient).generateJsonSummary(any(ReportGenerationPayloadDTO.class), eq("Bearer test-token"));
    }

    @Test
    @DisplayName("Should generate Excel report and dispatch report email attachment to requester")
    void shouldGenerateExcelAndDispatchEmail() {
        ReportSummaryRequestDTO request = new ReportSummaryRequestDTO();
        request.setReportType(ReportType.TICKET_SUMMARY);
        request.setFormat(ReportFormat.EXCEL);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(mockEmployee));

        DashboardAnalyticsResponseDTO mockAnalytics = DashboardAnalyticsResponseDTO.builder()
                .role(UserRole.ADMIN)
                .employeeId(1L)
                .build();
        when(analyticsService.getDashboardAnalytics(any(), any(), any())).thenReturn(mockAnalytics);

        byte[] fakeExcelBytes = "FAKE-EXCEL-DATA".getBytes();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "Admin_Ticket_Summary.xlsx");

        ResponseEntity<byte[]> mockResponse = ResponseEntity.ok().headers(headers).body(fakeExcelBytes);
        when(reportServiceClient.generateExcelReport(any(ReportGenerationPayloadDTO.class), anyString()))
                .thenReturn(mockResponse);

        ResponseEntity<byte[]> result = reportIntegrationService.getSummaryReportExcel(request, "Bearer test-token", adminPrincipal);

        assertNotNull(result);
        assertEquals(200, result.getStatusCode().value());
        assertArrayEquals(fakeExcelBytes, result.getBody());

        // Verify email delivery was triggered with the Excel attachment to the authenticated employee's registered email
        verify(notificationClient).sendReportEmailWithAttachment(
                eq("admin@company.com"),
                eq("Admin User"),
                contains("Help Desk Report"),
                anyString(),
                eq("Admin_Ticket_Summary.xlsx"),
                eq(fakeExcelBytes)
        );
    }

    @Test
    @DisplayName("Should permit AGENT to export AGENT_SUMMARY containing assigned tickets")
    void shouldPermitAgentToExportAgentSummary() {
        Employee agentEmployee = new Employee();
        agentEmployee.setId(2L);
        agentEmployee.setEmail("agent@company.com");
        agentEmployee.setFirstName("Bob");
        agentEmployee.setLastName("Agent");
        agentEmployee.setRole(UserRole.AGENT);
        agentEmployee.setTimezone("Asia/Kolkata");

        ReportSummaryRequestDTO request = new ReportSummaryRequestDTO();
        request.setReportType(ReportType.AGENT_SUMMARY);
        request.setFormat(ReportFormat.EXCEL);

        when(employeeRepository.findById(2L)).thenReturn(Optional.of(agentEmployee));

        DashboardAnalyticsResponseDTO mockAnalytics = DashboardAnalyticsResponseDTO.builder()
                .role(UserRole.AGENT)
                .employeeId(2L)
                .build();
        when(analyticsService.getDashboardAnalytics(any(), any(), any())).thenReturn(mockAnalytics);

        ExportTicketDetailDTO ticket = ExportTicketDetailDTO.builder()
                .id(100L)
                .ticketNumber("HD-2026-000100")
                .subject("Agent ticket")
                .assignedAgentName("Bob Agent")
                .build();
        when(analyticsService.getExportTicketDetails(any(), any(), any())).thenReturn(List.of(ticket));

        byte[] fakeExcelBytes = "AGENT-EXCEL-DATA".getBytes();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "agent-assigned-tickets-summary-" + LocalDate.now() + ".xlsx");

        ResponseEntity<byte[]> mockResponse = ResponseEntity.ok().headers(headers).body(fakeExcelBytes);
        when(reportServiceClient.generateExcelReport(any(ReportGenerationPayloadDTO.class), anyString()))
                .thenReturn(mockResponse);

        ResponseEntity<byte[]> result = reportIntegrationService.getSummaryReportExcel(request, "Bearer agent-token", agentPrincipal);

        assertNotNull(result);
        assertEquals(200, result.getStatusCode().value());
        assertArrayEquals(fakeExcelBytes, result.getBody());

        verify(reportServiceClient).generateExcelReport(argThat(payload -> {
            assertEquals(ReportType.AGENT_SUMMARY, payload.reportType());
            assertEquals("AGENT", payload.requestedByRole());
            assertEquals(2L, payload.requesterEmployeeId());
            assertEquals(1, payload.tickets().size());
            return true;
        }), eq("Bearer agent-token"));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when AGENT has no tickets matching criteria")
    void shouldThrowWhenAgentHasNoMatchingTickets() {
        Employee agentEmployee = new Employee();
        agentEmployee.setId(2L);
        agentEmployee.setEmail("agent@company.com");
        agentEmployee.setFirstName("Bob");
        agentEmployee.setRole(UserRole.AGENT);

        ReportSummaryRequestDTO request = new ReportSummaryRequestDTO();
        request.setReportType(ReportType.AGENT_SUMMARY);
        request.setFormat(ReportFormat.EXCEL);

        when(employeeRepository.findById(2L)).thenReturn(Optional.of(agentEmployee));
        when(analyticsService.getExportTicketDetails(any(), any(), any())).thenReturn(Collections.emptyList());

        assertThrows(xyz.mobi.employeehelpdesk.exception.ResourceNotFoundException.class, () ->
                reportIntegrationService.getSummaryReportExcel(request, "Bearer agent-token", agentPrincipal)
        );
    }

    @Test
    @DisplayName("Should prevent AGENT from accessing unauthorized report types like ADMIN_SUMMARY")
    void shouldPreventAgentFromAccessingAdminSummary() {
        Employee agentEmployee = new Employee();
        agentEmployee.setId(2L);
        agentEmployee.setRole(UserRole.AGENT);

        ReportSummaryRequestDTO request = new ReportSummaryRequestDTO();
        request.setReportType(ReportType.ADMIN_SUMMARY);
        request.setFormat(ReportFormat.EXCEL);

        when(employeeRepository.findById(2L)).thenReturn(Optional.of(agentEmployee));

        assertThrows(xyz.mobi.employeehelpdesk.exception.UnauthorizedException.class, () ->
                reportIntegrationService.getSummaryReportExcel(request, "Bearer agent-token", agentPrincipal)
        );
    }

    @Test
    @DisplayName("Should apply status, priority, and search filters to AGENT_SUMMARY tickets")
    void shouldFilterTicketsByStatusPriorityAndSearchForAgentSummary() {
        Employee agentEmployee = new Employee();
        agentEmployee.setId(2L);
        agentEmployee.setEmail("agent@company.com");
        agentEmployee.setFirstName("Bob");
        agentEmployee.setLastName("Agent");
        agentEmployee.setRole(UserRole.AGENT);

        ReportSummaryRequestDTO request = new ReportSummaryRequestDTO();
        request.setReportType(ReportType.AGENT_SUMMARY);
        request.setFormat(ReportFormat.EXCEL);
        request.setStatus(TicketStatus.IN_PROGRESS);
        request.setPriority(Priority.HIGH);
        request.setSearch("laptop");

        when(employeeRepository.findById(2L)).thenReturn(Optional.of(agentEmployee));

        DashboardAnalyticsResponseDTO mockAnalytics = DashboardAnalyticsResponseDTO.builder()
                .role(UserRole.AGENT)
                .employeeId(2L)
                .build();
        when(analyticsService.getDashboardAnalytics(any(), any(), any())).thenReturn(mockAnalytics);

        ExportTicketDetailDTO ticket1 = ExportTicketDetailDTO.builder()
                .id(101L)
                .ticketNumber("HD-2026-000101")
                .subject("Repair laptop display")
                .status(TicketStatus.IN_PROGRESS)
                .priority(Priority.HIGH)
                .assignedAgentName("Bob Agent")
                .build();

        ExportTicketDetailDTO ticket2 = ExportTicketDetailDTO.builder()
                .id(102L)
                .ticketNumber("HD-2026-000102")
                .subject("Printer toner low")
                .status(TicketStatus.RESOLVED)
                .priority(Priority.LOW)
                .assignedAgentName("Bob Agent")
                .build();

        when(analyticsService.getExportTicketDetails(any(), any(), any())).thenReturn(List.of(ticket1, ticket2));

        byte[] fakeExcelBytes = "FILTERED-EXCEL-DATA".getBytes();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "agent-assigned-tickets-summary-" + LocalDate.now() + ".xlsx");

        ResponseEntity<byte[]> mockResponse = ResponseEntity.ok().headers(headers).body(fakeExcelBytes);
        when(reportServiceClient.generateExcelReport(any(ReportGenerationPayloadDTO.class), anyString()))
                .thenReturn(mockResponse);

        ResponseEntity<byte[]> result = reportIntegrationService.getSummaryReportExcel(request, "Bearer agent-token", agentPrincipal);

        assertNotNull(result);
        assertEquals(200, result.getStatusCode().value());
        verify(reportServiceClient).generateExcelReport(argThat(payload -> {
            assertEquals(1, payload.tickets().size());
            assertEquals(101L, payload.tickets().get(0).id());
            assertEquals("HD-2026-000101", payload.tickets().get(0).ticketNumber());
            return true;
        }), eq("Bearer agent-token"));
    }
}
