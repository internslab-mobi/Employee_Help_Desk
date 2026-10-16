package xyz.mobi.Employee_Help_Desk_Auth.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
public class NotificationClient {
    private final RestClient restClient;

    public NotificationClient(RestClient.Builder builder,
                              @Value("${notification.service.base-url}") String baseUrl){
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public void sendNotificationEmail(String email, String subject, String message){
        EmailRequestDTO request = new EmailRequestDTO(email, subject, message);

        restClient.post()
                .uri("/notifications/send")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();

        log.info("OTP email request accepted by Notification Service");

    }

    private record EmailRequestDTO(String to, String subject, String body){}
}
