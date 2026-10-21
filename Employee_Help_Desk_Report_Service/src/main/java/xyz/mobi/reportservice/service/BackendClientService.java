package xyz.mobi.reportservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import xyz.mobi.reportservice.dto.DashboardAnalyticsResponseDTO;
import xyz.mobi.reportservice.dto.ExportTicketDetailDTO;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class BackendClientService {

    private final RestTemplate restTemplate;
    private final String backendBaseUrl;

    public BackendClientService(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${helpdesk.backend.url:http://localhost:8082}") String backendBaseUrl
    ) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
        this.backendBaseUrl = backendBaseUrl.replaceAll("/+$", "");
    }

    public DashboardAnalyticsResponseDTO fetchDashboardAnalytics(
            String rawToken, LocalDate fromDate, LocalDate toDate, Long departmentId
    ) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
<<<<<<< HEAD
                .fromHttpUrl(backendBaseUrl + "/helpdesk/analytics/dashboard");
=======
                .fromHttpUrl(backendBaseUrl + "/analytics/dashboard");
>>>>>>> 22515536 (microservices summary report)

        if (fromDate != null) uriBuilder.queryParam("fromDate", fromDate.toString());
        if (toDate != null) uriBuilder.queryParam("toDate", toDate.toString());
        if (departmentId != null) uriBuilder.queryParam("departmentId", departmentId);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(rawToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<DashboardAnalyticsResponseDTO> response = restTemplate.exchange(
                    uriBuilder.toUriString(),
                    HttpMethod.GET,
                    requestEntity,
                    DashboardAnalyticsResponseDTO.class
            );
            return response.getBody();
        } catch (HttpStatusCodeException ex) {
            log.error("Downstream backend error fetching analytics: status={}, body={}",
                    ex.getStatusCode(), ex.getResponseBodyAsString());
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to connect to backend: {}", ex.getMessage(), ex);
            throw new RuntimeException("Backend communication failed: " + ex.getMessage(), ex);
        }
    }

    public List<ExportTicketDetailDTO> fetchExportTicketDetails(
            String rawToken, LocalDate fromDate, LocalDate toDate, Long departmentId
    ) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
<<<<<<< HEAD
                .fromHttpUrl(backendBaseUrl + "/helpdesk/analytics/export-data");
=======
                .fromHttpUrl(backendBaseUrl + "/analytics/export-data");
>>>>>>> 22515536 (microservices summary report)

        if (fromDate != null) uriBuilder.queryParam("fromDate", fromDate.toString());
        if (toDate != null) uriBuilder.queryParam("toDate", toDate.toString());
        if (departmentId != null) uriBuilder.queryParam("departmentId", departmentId);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(rawToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<List<ExportTicketDetailDTO>> response = restTemplate.exchange(
                    uriBuilder.toUriString(),
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<>() {}
            );
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (HttpStatusCodeException ex) {
            log.error("Downstream backend error fetching ticket details: status={}, body={}",
                    ex.getStatusCode(), ex.getResponseBodyAsString());
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to connect to backend for ticket export: {}", ex.getMessage(), ex);
            throw new RuntimeException("Backend communication failed: " + ex.getMessage(), ex);
        }
    }
}
