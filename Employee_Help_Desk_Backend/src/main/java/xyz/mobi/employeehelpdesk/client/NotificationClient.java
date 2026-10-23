package xyz.mobi.employeehelpdesk.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Base64;

@Slf4j
@Component
public class NotificationClient {

    private final RestClient restClient;

    public NotificationClient(
            RestClient.Builder builder,
            @Value("${notification.service.base-url:http://localhost:8085}") String baseUrl
    ) {
        this.restClient = builder.baseUrl(baseUrl.replaceAll("/+$", "")).build();
        log.info("Initialized NotificationClient with base URL: {}", baseUrl);
    }

    @Async
    public void sendNotificationEmail(String email, String subject, String message) {
        if (email == null || email.isBlank()) {
            log.warn("Cannot send notification email: recipient email is null/empty");
            return;
        }

        try {
            EmailRequestDTO request = new EmailRequestDTO(email.trim(), subject, message, null, null);

            restClient.post()
                    .uri("/notifications/send")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Notification email dispatched to Notification Service for recipient: {}", email);
        } catch (Exception ex) {
            log.error("Failed to dispatch notification email to Notification Service for {}: {}", email, ex.getMessage(), ex);
        }
    }

    @Async
    public void sendReportEmailWithAttachment(
            String recipientEmail,
            String recipientName,
            String subject,
            String body,
            String attachmentFilename,
            byte[] attachmentData
    ) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.warn("Cannot send report email: recipient email is null/empty");
            return;
        }

        try {
            String base64Data = (attachmentData != null && attachmentData.length > 0)
                    ? Base64.getEncoder().encodeToString(attachmentData)
                    : null;

            EmailRequestDTO request = new EmailRequestDTO(
                    recipientEmail.trim(),
                    subject,
                    body,
                    attachmentFilename,
                    base64Data
            );

            restClient.post()
                    .uri("/notifications/send")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Report email with attachment '{}' dispatched to Notification Service for recipient: {}",
                    attachmentFilename, recipientEmail);
        } catch (Exception ex) {
            log.error("Failed to dispatch report email with attachment to Notification Service for {}: {}",
                    recipientEmail, ex.getMessage(), ex);
        }
    }

    public record EmailRequestDTO(
            String to,
            String subject,
            String body,
            String attachmentFilename,
            String attachmentBase64
    ) {}
}
