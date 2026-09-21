package xyz.mobi.employeehelpdesk.dto.notification;

import xyz.mobi.employeehelpdesk.entity.enums.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        Long recipientId,
        Long ticketId,
        String ticketNumber,
        NotificationType type,
        String title,
        String message,
        Boolean read,
        LocalDateTime createdAt
) {
}
