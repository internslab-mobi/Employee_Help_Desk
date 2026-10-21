package xyz.mobi.employeehelpdesk.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import xyz.mobi.employeehelpdesk.dto.report.ReportGenerationPayloadDTO;
import xyz.mobi.employeehelpdesk.dto.report.ReportSummaryResponseDTO;
import xyz.mobi.employeehelpdesk.exception.HelpdeskException;
import xyz.mobi.employeehelpdesk.exception.ServiceUnavailableException;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
public class ReportServiceClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public ReportServiceClient(
            @Value("${report.service.base-url:http://localhost:8083}") String baseUrl,
            @Value("${report.service.connect-timeout-seconds:5}") int connectTimeoutSeconds,
            @Value("${report.service.read-timeout-seconds:30}") int readTimeoutSeconds
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(connectTimeoutSeconds));
        factory.setReadTimeout(Duration.ofSeconds(readTimeoutSeconds));

        this.restTemplate = new RestTemplate(factory);
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        log.info("Initialized ReportServiceClient with downstream URL: {}", this.baseUrl);
    }

    /**
     * Request JSON summary calculation from Report Microservice.
     */
    public ReportSummaryResponseDTO generateJsonSummary(ReportGenerationPayloadDTO payload, String rawToken) {
        String url = baseUrl + "/helpdesk/reports/generate";

        HttpHeaders headers = createHeaders(rawToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<ReportGenerationPayloadDTO> requestEntity = new HttpEntity<>(payload, headers);

        try {
            log.debug("Invoking Report Microservice for JSON summary: type={}, role={}",
                    payload.reportType(), payload.requestedByRole());

            ResponseEntity<ReportSummaryResponseDTO> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    ReportSummaryResponseDTO.class
            );

            if (response.getBody() == null) {
                throw new HelpdeskException("Report microservice returned an empty response body.");
            }
            return response.getBody();

        } catch (ResourceAccessException ex) {
            log.error("Failed to connect to report microservice at {}: {}", url, ex.getMessage());
            throw new ServiceUnavailableException("Report summary microservice is currently unavailable. Please verify service health.", ex);
        } catch (HttpStatusCodeException ex) {
            log.error("Report microservice returned error status {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new HelpdeskException("Report microservice failed to generate summary: " + ex.getStatusText(), ex);
        } catch (Exception ex) {
            log.error("Unexpected error invoking report microservice: {}", ex.getMessage(), ex);
            throw new HelpdeskException("Failed to generate summary report: " + ex.getMessage(), ex);
        }
    }

    /**
     * Request Excel workbook generation from Report Microservice.
     */
    public ResponseEntity<byte[]> generateExcelReport(ReportGenerationPayloadDTO payload, String rawToken) {
        String url = baseUrl + "/helpdesk/reports/generate";

        HttpHeaders headers = createHeaders(rawToken);
        headers.setAccept(List.of(
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
                MediaType.APPLICATION_OCTET_STREAM
        ));
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<ReportGenerationPayloadDTO> requestEntity = new HttpEntity<>(payload, headers);

        try {
            log.debug("Invoking Report Microservice for Excel workbook: type={}, role={}",
                    payload.reportType(), payload.requestedByRole());

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    byte[].class
            );

            byte[] bytes = response.getBody();
            if (bytes == null || bytes.length == 0) {
                throw new HelpdeskException("Report microservice returned empty workbook bytes.");
            }

            return response;

        } catch (ResourceAccessException ex) {
            log.error("Failed to connect to report microservice at {}: {}", url, ex.getMessage());
            throw new ServiceUnavailableException("Report summary microservice is currently unavailable. Please verify service health.", ex);
        } catch (HttpStatusCodeException ex) {
            log.error("Report microservice returned error status {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new HelpdeskException("Report microservice failed to generate Excel report: " + ex.getStatusText(), ex);
        } catch (Exception ex) {
            log.error("Unexpected error invoking report microservice for Excel: {}", ex.getMessage(), ex);
            throw new HelpdeskException("Failed to generate Excel report: " + ex.getMessage(), ex);
        }
    }

    private HttpHeaders createHeaders(String rawToken) {
        HttpHeaders headers = new HttpHeaders();
        if (rawToken != null && !rawToken.isBlank()) {
            String token = rawToken.startsWith("Bearer ") ? rawToken.substring(7).trim() : rawToken.trim();
            headers.setBearerAuth(token);
        }
        return headers;
    }
}
