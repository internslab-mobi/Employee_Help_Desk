package xyz.mobi.notificationservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import xyz.mobi.notificationservice.entity.enums.NotificationType;

@Builder
public record CreateNotificationRequestDTO(
        @NotNull(message = "Recipient ID is required")
        Long recipientId,

        @Email(message = "Recipient email must be a valid email address")
        String recipientEmail,

        Long ticketId,

        @Size(max = 50, message = "Ticket number cannot exceed 50 characters")
        String ticketNumber,

        @NotNull(message = "Notification type is required")
        NotificationType type,

        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title cannot exceed 255 characters")
        String title,

        @NotBlank(message = "Message is required")
        String message
) {
}
