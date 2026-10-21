package xyz.mobi.notificationservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.Instant;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EmailResponseDTO(
        String status,
        String message,
        Boolean success,
        String recipient,
        Instant timestamp
) {
    public static EmailResponseDTO success(String recipient, String message) {
        return EmailResponseDTO.builder()
                .status("SUCCESS")
                .message(message)
                .success(true)
                .recipient(recipient)
                .timestamp(Instant.now())
                .build();
    }
}
