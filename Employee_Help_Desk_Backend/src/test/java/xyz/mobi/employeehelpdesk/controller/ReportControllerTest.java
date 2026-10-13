package xyz.mobi.employeehelpdesk.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import xyz.mobi.employeehelpdesk.dto.report.ReportFormat;
import xyz.mobi.employeehelpdesk.dto.report.ReportSummaryRequestDTO;
import xyz.mobi.employeehelpdesk.dto.report.ReportSummaryResponseDTO;
import xyz.mobi.employeehelpdesk.dto.report.ReportType;
import xyz.mobi.employeehelpdesk.entity.enums.Priority;
import xyz.mobi.employeehelpdesk.entity.enums.TicketStatus;
import xyz.mobi.employeehelpdesk.exception.GlobalExceptionHandler;
import xyz.mobi.employeehelpdesk.service.ReportIntegrationService;
import xyz.mobi.employeehelpdesk.service.helperservice.ErrorCodeCacheService;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class ReportControllerTest {

    @Mock
    private ReportIntegrationService reportIntegrationService;

    @Mock
    private ErrorCodeCacheService errorCodeCacheService;

    @InjectMocks
    private ReportController reportController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        GlobalExceptionHandler advice = new GlobalExceptionHandler(errorCodeCacheService);
        mockMvc = MockMvcBuilders.standaloneSetup(reportController)
                .setControllerAdvice(advice)
                .build();
    }

    @Test
    void testGetSummaryReportJson_ViaMockMvcQueryParameters() throws Exception {
        ReportSummaryResponseDTO mockResponse = ReportSummaryResponseDTO.builder()
                .reportType(ReportType.GENERAL_SUMMARY)
                .format(ReportFormat.JSON)
                .totalRecords(25L)
                .build();

        doReturn(ResponseEntity.ok(mockResponse))
                .when(reportIntegrationService).getSummaryReport(any(ReportSummaryRequestDTO.class), eq("Bearer token123"));

        mockMvc.perform(get("/api/reports/summary")
                        .param("reportType", "GENERAL_SUMMARY")
                        .param("format", "JSON")
                        .param("fromDate", "2026-10-01")
                        .param("toDate", "2026-10-12")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token123"))
                .andExpect(status().isOk());

        ArgumentCaptor<ReportSummaryRequestDTO> captor = ArgumentCaptor.forClass(ReportSummaryRequestDTO.class);
        verify(reportIntegrationService, times(1)).getSummaryReport(captor.capture(), eq("Bearer token123"));

        ReportSummaryRequestDTO captured = captor.getValue();
        assertEquals(ReportType.GENERAL_SUMMARY, captured.getReportType());
        assertEquals(ReportFormat.JSON, captured.getFormat());
        assertEquals(LocalDate.of(2026, 10, 1), captured.getFromDate());
        assertEquals(LocalDate.of(2026, 10, 12), captured.getToDate());
    }

    @Test
    void testGetSummaryReportExcel_ViaMockMvcQueryParameters() throws Exception {
        byte[] excelBytes = new byte[]{0x50, 0x4B, 0x03, 0x04};
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "Ticket_Summary.xlsx");

        doReturn(ResponseEntity.ok().headers(headers).body(excelBytes))
                .when(reportIntegrationService).getSummaryReport(any(ReportSummaryRequestDTO.class), eq("Bearer token123"));

        mockMvc.perform(get("/api/reports/summary")
                        .param("reportType", "TICKET_SUMMARY")
                        .param("format", "EXCEL")
                        .param("status", "OPEN")
                        .param("priority", "HIGH")
                        .param("departmentId", "1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token123"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(content().bytes(excelBytes));

        ArgumentCaptor<ReportSummaryRequestDTO> captor = ArgumentCaptor.forClass(ReportSummaryRequestDTO.class);
        verify(reportIntegrationService, times(1)).getSummaryReport(captor.capture(), eq("Bearer token123"));

        ReportSummaryRequestDTO captured = captor.getValue();
        assertEquals(ReportType.TICKET_SUMMARY, captured.getReportType());
        assertEquals(ReportFormat.EXCEL, captured.getFormat());
        assertEquals(TicketStatus.OPEN, captured.getStatus());
        assertEquals(Priority.HIGH, captured.getPriority());
        assertEquals(1L, captured.getDepartmentId());
    }

    @Test
    void testGetSummaryReport_InvalidDateRange_ReturnsBadRequest() throws Exception {
        when(errorCodeCacheService.getErrorCode(any())).thenReturn("ERR_VALIDATION");

        mockMvc.perform(get("/api/reports/summary")
                        .param("reportType", "GENERAL_SUMMARY")
                        .param("fromDate", "2026-10-15")
                        .param("toDate", "2026-10-01") // fromDate after toDate
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token123"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(reportIntegrationService);
    }

    @Test
    void testPostSummaryReport_MethodNotAllowed() throws Exception {
        when(errorCodeCacheService.getErrorCode(any())).thenReturn("ERR_METHOD_NOT_ALLOWED");

        mockMvc.perform(post("/api/reports/summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token123"))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(reportIntegrationService);
    }

    @Test
    void testGetSummaryReport_DirectMethodInvocation() {
        ReportSummaryRequestDTO request = ReportSummaryRequestDTO.builder()
                .reportType(ReportType.GENERAL_SUMMARY)
                .format(ReportFormat.JSON)
                .build();

        ReportSummaryResponseDTO expectedResponse = ReportSummaryResponseDTO.builder()
                .reportType(ReportType.GENERAL_SUMMARY)
                .format(ReportFormat.JSON)
                .totalRecords(25L)
                .build();

        doReturn(ResponseEntity.ok(expectedResponse))
                .when(reportIntegrationService).getSummaryReport(eq(request), eq("Bearer token123"));

        ResponseEntity<?> response = reportController.getSummaryReport(request, "Bearer token123");

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedResponse, response.getBody());
        verify(reportIntegrationService, times(1)).getSummaryReport(eq(request), eq("Bearer token123"));
    }
}
