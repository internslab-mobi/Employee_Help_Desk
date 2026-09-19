package com.example.helpdesk.service.impl;

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

import java.time.LocalDateTime;
import java.util.List;

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
                .type(type != null ? type.name() : null)
                .title(title)
                .message(message)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);
        log.info("Sent notification to employee {} for ticket {}: type={}", 
                recipient.getId(), ticket != null ? ticket.getId() : "N/A", type);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setReadAt(LocalDateTime.now());
            notification.setUpdatedAt(LocalDateTime.now());
            notificationRepository.save(notification);
            log.info("Marked notification {} as read", notificationId);
        });
    }

    @Override
    @Transactional
    public void markAllAsReadForEmployee(Long employeeId) {
        List<Notification> unreadNotifications = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(employeeId);
        unreadNotifications.stream()
                .filter(n -> n.getReadAt() == null)
                .forEach(n -> {
                    n.setReadAt(LocalDateTime.now());
                    n.setUpdatedAt(LocalDateTime.now());
                    notificationRepository.save(n);
                });
        log.info("Marked all notifications as read for employee {}", employeeId);
    }
}
