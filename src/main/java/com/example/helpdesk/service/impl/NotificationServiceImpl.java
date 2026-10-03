package com.example.helpdesk.service.impl;

import com.example.helpdesk.dto.response.NotificationResponseDTO;
import com.example.helpdesk.entity.Employee;
import com.example.helpdesk.entity.Notification;
import com.example.helpdesk.entity.Ticket;
import com.example.helpdesk.enums.NotificationType;
import com.example.helpdesk.repository.NotificationRepository;
import com.example.helpdesk.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public void sendNotification(Employee recipient, Ticket ticket, NotificationType type, String title, String message) {
        Notification notification = Notification.builder()
                .recipient(recipient)
                .ticket(ticket)
                .type(type.name())
                .title(title)
                .message(message)
                .readAt(null)
                .build();

        notificationRepository.save(notification);
        log.info("Sent notification to employee {} of type {}", recipient.getEmail(), type);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found with id: " + notificationId));

        notification.setReadAt(Instant.now());
        notificationRepository.save(notification);
        log.info("Marked notification {} as read", notificationId);
    }

    @Override
    @Transactional
    public void markAllAsReadForEmployee(Long employeeId) {
        List<Notification> notifications = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(employeeId);
        Instant now = Instant.now();

        notifications.stream()
                .filter(n -> n.getReadAt() == null)
                .forEach(n -> n.setReadAt(now));

        notificationRepository.saveAll(notifications);
        log.info("Marked all notifications as read for employee {}", employeeId);
    }

    @Override
    public List<NotificationResponseDTO> getNotificationsByEmployee(Long employeeId) {
        List<Notification> notifications = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(employeeId);

        return notifications.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private NotificationResponseDTO mapToDTO(Notification notification) {
        return NotificationResponseDTO.builder()
                .id(notification.getId())
                .recipientId(notification.getRecipient().getId())
                .ticketId(notification.getTicket() != null ? notification.getTicket().getId() : null)
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .createdAt(toOffsetDateTime(notification.getCreatedAt()))
                .readAt(notification.getReadAt() != null ? toOffsetDateTime(notification.getReadAt()) : null)
                .build();
    }

    private OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant != null ? instant.atOffset(ZoneOffset.UTC) : null;
    }
}
