package xyz.mobi.employeehelpdesk.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import xyz.mobi.employeehelpdesk.dto.report.ReportFormat;
import xyz.mobi.employeehelpdesk.dto.report.ReportGenerationPayloadDTO;
import xyz.mobi.employeehelpdesk.dto.report.ReportSummaryResponseDTO;
import xyz.mobi.employeehelpdesk.dto.report.ReportType;
import xyz.mobi.employeehelpdesk.exception.HelpdeskException;
import xyz.mobi.employeehelpdesk.exception.ServiceUnavailableException;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

public class ReportServiceClientTest {

    private ReportServiceClient client;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        client = new ReportServiceClient("http://localhost:8083", 5, 30);
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
        assertNotNull(restTemplate);
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void testGenerateJsonSummary_Success() {
        mockServer.expect(requestTo("http://localhost:8083/api/reports/generate"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andRespond(withSuccess(
                        "{\"reportType\":\"GENERAL_SUMMARY\",\"format\":\"JSON\",\"totalRecords\":10}",
                        MediaType.APPLICATION_JSON
                ));

        ReportGenerationPayloadDTO payload = ReportGenerationPayloadDTO.builder()
                .reportType(ReportType.GENERAL_SUMMARY)
                .format(ReportFormat.JSON)
                .build();

        ReportSummaryResponseDTO response = client.generateJsonSummary(payload, "Bearer test-token");

        assertNotNull(response);
        assertEquals(ReportType.GENERAL_SUMMARY, response.getReportType());
        assertEquals(10L, response.getTotalRecords());
        mockServer.verify();
    }

    @Test
    void testGenerateExcelReport_Success() {
        byte[] fakeExcel = new byte[]{1, 2, 3, 4};
        mockServer.expect(requestTo("http://localhost:8083/api/reports/generate"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andRespond(withSuccess(
                        fakeExcel,
                        MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                ));

        ReportGenerationPayloadDTO payload = ReportGenerationPayloadDTO.builder()
                .reportType(ReportType.TICKET_SUMMARY)
                .format(ReportFormat.EXCEL)
                .build();

        ResponseEntity<byte[]> response = client.generateExcelReport(payload, "Bearer test-token");

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertArrayEquals(fakeExcel, response.getBody());
        mockServer.verify();
    }

    @Test
    void testDownstream500_ThrowsHelpdeskException() {
        mockServer.expect(requestTo("http://localhost:8083/api/reports/generate"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        ReportGenerationPayloadDTO payload = ReportGenerationPayloadDTO.builder()
                .reportType(ReportType.GENERAL_SUMMARY)
                .format(ReportFormat.JSON)
                .build();

        HelpdeskException ex = assertThrows(HelpdeskException.class, () ->
                client.generateJsonSummary(payload, "Bearer test-token")
        );
        assertTrue(ex.getMessage().contains("Report microservice failed to generate summary"));
        mockServer.verify();
    }
}
