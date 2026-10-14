package xyz.mobi.employeehelpdesk.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import xyz.mobi.employeehelpdesk.client.ReportServiceClient;
import xyz.mobi.employeehelpdesk.dto.analytics.DashboardAnalyticsResponseDTO;
import xyz.mobi.employeehelpdesk.dto.analytics.DashboardSummaryDTO;
import xyz.mobi.employeehelpdesk.dto.analytics.ExportTicketDetailDTO;
import xyz.mobi.employeehelpdesk.dto.report.*;
import xyz.mobi.employeehelpdesk.entity.Department;
import xyz.mobi.employeehelpdesk.entity.Employee;
import xyz.mobi.employeehelpdesk.entity.enums.Priority;
import xyz.mobi.employeehelpdesk.entity.enums.TicketStatus;
import xyz.mobi.employeehelpdesk.entity.enums.UserRole;
import xyz.mobi.employeehelpdesk.exception.UnauthorizedException;
import xyz.mobi.employeehelpdesk.repository.EmployeeRepository;
import xyz.mobi.employeehelpdesk.service.helperservice.EmailService;
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
public class ReportIntegrationServiceTest {

    @Mock
    private AuthService authService;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private AnalyticsService analyticsService;

    @Mock
    private ReportServiceClient reportServiceClient;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private ReportIntegrationServiceImpl reportIntegrationService;

    private Employee employeeUser;
    private Employee agentUser;
    private Employee managerUser;
    private Employee adminUser;
    private Department itDepartment;

    @BeforeEach
    void setUp() {
        itDepartment = new Department();
        itDepartment.setId(10L);
        itDepartment.setName("IT Support");

        employeeUser = new Employee();
        employeeUser.setId(1L);
        employeeUser.setFirstName("John");
        employeeUser.setLastName("Doe");
        employeeUser.setEmail("john.doe@company.com");
        employeeUser.setRole(UserRole.EMPLOYEE);
        employeeUser.setTimezone("UTC");
        employeeUser.setDepartment(itDepartment);

        agentUser = new Employee();
        agentUser.setId(2L);
        agentUser.setFirstName("Agent");
        agentUser.setLastName("Smith");
        agentUser.setEmail("agent.smith@company.com");
        agentUser.setRole(UserRole.AGENT);
        agentUser.setTimezone("UTC");
        agentUser.setDepartment(itDepartment);

        managerUser = new Employee();
        managerUser.setId(3L);
        managerUser.setFirstName("Manager");
        managerUser.setLastName("Boss");
        managerUser.setEmail("manager@company.com");
        managerUser.setRole(UserRole.MANAGER);
        managerUser.setTimezone("UTC");
        managerUser.setDepartment(itDepartment);

        adminUser = new Employee();
        adminUser.setId(4L);
        adminUser.setFirstName("Admin");
        adminUser.setLastName("Super");
        adminUser.setEmail("admin@company.com");
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setTimezone("UTC");

    }

    @Test
    void testEmployeeAccessGeneralSummary_Success() {
        when(authService.getCurrentEmployeeId()).thenReturn(1L);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employeeUser));

        DashboardAnalyticsResponseDTO analytics = DashboardAnalyticsResponseDTO.builder()
                .role(UserRole.EMPLOYEE)
                .employeeId(1L)
                .summary(DashboardSummaryDTO.builder().totalTickets(5L).openTickets(2L).resolvedTickets(3L).build())
                .build();

        when(analyticsService.getDashboardAnalytics(any(), any(), isNull())).thenReturn(analytics);
        when(analyticsService.getExportTicketDetails(any(), any(), isNull())).thenReturn(Collections.emptyList());

        ReportSummaryResponseDTO expectedResponse = ReportSummaryResponseDTO.builder()
                .reportType(ReportType.GENERAL_SUMMARY)
                .format(ReportFormat.JSON)
                .totalRecords(5L)
                .build();

        when(reportServiceClient.generateJsonSummary(any(), anyString())).thenReturn(expectedResponse);

        ReportSummaryRequestDTO request = ReportSummaryRequestDTO.builder()
                .reportType(ReportType.GENERAL_SUMMARY)
                .format(ReportFormat.JSON)
                .fromDate(LocalDate.of(2026, 10, 1))
                .toDate(LocalDate.of(2026, 10, 12))
                .build();

        ReportSummaryResponseDTO result = reportIntegrationService.getSummaryReportJson(request, "Bearer mock-token");

        assertNotNull(result);
        assertEquals(ReportType.GENERAL_SUMMARY, result.getReportType());
        assertEquals(5L, result.getTotalRecords());
        verify(reportServiceClient, times(1)).generateJsonSummary(any(), eq("Bearer mock-token"));
    }

    @Test
    void testEmployeeForbiddenFromAdminSummary() {
        when(authService.getCurrentEmployeeId()).thenReturn(1L);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employeeUser));

        ReportSummaryRequestDTO request = ReportSummaryRequestDTO.builder()
                .reportType(ReportType.ADMIN_SUMMARY)
                .format(ReportFormat.JSON)
                .build();

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () ->
                reportIntegrationService.getSummaryReportJson(request, "Bearer token")
        );
        assertTrue(ex.getMessage().contains("Employees are not authorized to access ADMIN_SUMMARY"));
        verifyNoInteractions(analyticsService);
        verifyNoInteractions(reportServiceClient);
    }

    @Test
    void testAgentForbiddenFromManagerSummary() {
        when(authService.getCurrentEmployeeId()).thenReturn(2L);
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(agentUser));

        ReportSummaryRequestDTO request = ReportSummaryRequestDTO.builder()
                .reportType(ReportType.MANAGER_SUMMARY)
                .format(ReportFormat.JSON)
                .build();

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () ->
                reportIntegrationService.getSummaryReportJson(request, "Bearer token")
        );
        assertTrue(ex.getMessage().contains("Agents are not authorized to access MANAGER_SUMMARY"));
    }

    @Test
    void testManagerForbiddenFromAccessingOtherDepartment() {
        when(authService.getCurrentEmployeeId()).thenReturn(3L);
        when(employeeRepository.findById(3L)).thenReturn(Optional.of(managerUser));

        ReportSummaryRequestDTO request = ReportSummaryRequestDTO.builder()
                .reportType(ReportType.MANAGER_SUMMARY)
                .format(ReportFormat.JSON)
                .departmentId(999L) // Unauthorized foreign department
                .build();

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () ->
                reportIntegrationService.getSummaryReportJson(request, "Bearer token")
        );
        assertTrue(ex.getMessage().contains("Managers are not authorized to access reports for another department"));
    }

    @Test
    void testManagerAuthorizedDepartmentSummary_Success() {
        when(authService.getCurrentEmployeeId()).thenReturn(3L);
        when(employeeRepository.findById(3L)).thenReturn(Optional.of(managerUser));

        when(analyticsService.getDashboardAnalytics(any(), any(), eq(10L))).thenReturn(
                DashboardAnalyticsResponseDTO.builder().role(UserRole.MANAGER).build()
        );
        when(analyticsService.getExportTicketDetails(any(), any(), eq(10L))).thenReturn(Collections.emptyList());

        when(reportServiceClient.generateJsonSummary(any(), anyString())).thenReturn(
                ReportSummaryResponseDTO.builder().reportType(ReportType.MANAGER_SUMMARY).build()
        );

        ReportSummaryRequestDTO request = ReportSummaryRequestDTO.builder()
                .reportType(ReportType.MANAGER_SUMMARY)
                .format(ReportFormat.JSON)
                .departmentId(10L) // Valid managed department
                .build();

        ReportSummaryResponseDTO result = reportIntegrationService.getSummaryReportJson(request, "Bearer token");
        assertNotNull(result);
        assertEquals(ReportType.MANAGER_SUMMARY, result.getReportType());
    }

    @Test
    void testAdminAuthorizedForAllReportsAndExcelDownload() {
        when(authService.getCurrentEmployeeId()).thenReturn(4L);
        when(employeeRepository.findById(4L)).thenReturn(Optional.of(adminUser));

        when(analyticsService.getDashboardAnalytics(any(), any(), isNull())).thenReturn(
                DashboardAnalyticsResponseDTO.builder().role(UserRole.ADMIN).build()
        );

        ExportTicketDetailDTO ticket1 = ExportTicketDetailDTO.builder()
                .id(101L)
                .ticketNumber("TKT-101")
                .status(TicketStatus.OPEN)
                .priority(Priority.HIGH)
                .build();

        ExportTicketDetailDTO ticket2 = ExportTicketDetailDTO.builder()
                .id(102L)
                .ticketNumber("TKT-102")
                .status(TicketStatus.RESOLVED)
                .priority(Priority.LOW)
                .build();

        when(analyticsService.getExportTicketDetails(any(), any(), isNull())).thenReturn(List.of(ticket1, ticket2));

        byte[] mockExcel = new byte[]{1, 2, 3, 4, 5};
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "Admin_Summary_2026-10-12.xlsx");

        when(reportServiceClient.generateExcelReport(any(), anyString())).thenReturn(
                ResponseEntity.ok().headers(headers).body(mockExcel)
        );

        ReportSummaryRequestDTO request = ReportSummaryRequestDTO.builder()
                .reportType(ReportType.ADMIN_SUMMARY)
                .format(ReportFormat.EXCEL)
                .status(TicketStatus.OPEN) // Filter test
                .build();

        ResponseEntity<byte[]> response = reportIntegrationService.getSummaryReportExcel(request, "Bearer admin-token");

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(5, response.getBody().length);
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", response.getHeaders().getContentType().toString());

        verify(emailService, times(1)).sendReportEmailWithAttachment(
                eq("admin@company.com"),
                eq("Admin Super"),
                contains("ADMIN SUMMARY"),
                contains("Dear Admin Super"),
                eq("Admin_Summary_2026-10-12.xlsx"),
                eq(mockExcel)
        );
    }
}
