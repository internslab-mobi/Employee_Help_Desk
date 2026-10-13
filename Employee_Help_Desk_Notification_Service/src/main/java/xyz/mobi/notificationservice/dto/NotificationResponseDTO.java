package xyz.mobi.notificationservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import xyz.mobi.notificationservice.entity.enums.NotificationType;

import java.time.Instant;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record NotificationResponseDTO(
        Long id,
        Long recipientId,
        Long ticketId,
        String ticketNumber,
        NotificationType type,
        String title,
        String message,
        Boolean read,
        Instant createdAt,
        Instant updatedAt
) {
}
