package xyz.mobi.notificationservice.mapper;

import org.springframework.stereotype.Component;
import xyz.mobi.notificationservice.dto.CreateNotificationRequestDTO;
import xyz.mobi.notificationservice.dto.NotificationResponseDTO;
import xyz.mobi.notificationservice.entity.Notification;

@Component
public class NotificationMapper {

    public Notification toEntity(CreateNotificationRequestDTO request) {
        if (request == null) {
            return null;
        }

        Notification notification = new Notification();
        notification.setRecipientId(request.recipientId());
        notification.setTicketId(request.ticketId());
        notification.setTicketNumber(request.ticketNumber());
        notification.setType(request.type());
        notification.setTitle(request.title());
        notification.setMessage(request.message());
        notification.setRead(false);
        return notification;
    }

    public NotificationResponseDTO toResponse(Notification notification) {
        if (notification == null) {
            return null;
        }

        return NotificationResponseDTO.builder()
                .id(notification.getId())
                .recipientId(notification.getRecipientId())
                .ticketId(notification.getTicketId())
                .ticketNumber(notification.getTicketNumber())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .read(notification.getRead())
                .createdAt(notification.getCreatedAt())
                .updatedAt(notification.getUpdatedAt())
                .build();
    }

    public NotificationResponseDTO toCreateResponse(Notification notification) {
        if (notification == null) {
            return null;
        }

        return NotificationResponseDTO.builder()
                .id(notification.getId())
                .recipientId(notification.getRecipientId())
                .ticketId(notification.getTicketId())
                .ticketNumber(notification.getTicketNumber())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .read(notification.getRead())
                .createdAt(notification.getCreatedAt())
                .updatedAt(null)
                .build();
    }

    public NotificationResponseDTO toUpdateResponse(Notification notification) {
        if (notification == null) {
            return null;
        }

        return NotificationResponseDTO.builder()
                .id(notification.getId())
                .recipientId(notification.getRecipientId())
                .ticketId(notification.getTicketId())
                .ticketNumber(notification.getTicketNumber())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .read(notification.getRead())
                .createdAt(null)
                .updatedAt(notification.getUpdatedAt())
                .build();
    }
}
