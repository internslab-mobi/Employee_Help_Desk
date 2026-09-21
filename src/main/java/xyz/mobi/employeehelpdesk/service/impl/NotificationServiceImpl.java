package xyz.mobi.employeehelpdesk.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.mobi.employeehelpdesk.dto.notification.NotificationResponse;
import xyz.mobi.employeehelpdesk.dto.notification.UnreadNotificationCountResponse;
import xyz.mobi.employeehelpdesk.entity.Employee;
import xyz.mobi.employeehelpdesk.entity.Notification;
import xyz.mobi.employeehelpdesk.entity.Ticket;
import xyz.mobi.employeehelpdesk.entity.enums.NotificationType;
import xyz.mobi.employeehelpdesk.exception.BadRequestException;
import xyz.mobi.employeehelpdesk.exception.ResourceNotFoundException;
import xyz.mobi.employeehelpdesk.mapper.NotificationMapper;
import xyz.mobi.employeehelpdesk.repository.NotificationRepository;
import xyz.mobi.employeehelpdesk.service.CurrentUserService;
import xyz.mobi.employeehelpdesk.service.EmailService;
import xyz.mobi.employeehelpdesk.service.NotificationService;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final EmailService emailService;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public Notification sendNotification(
            Employee recipient,
            Ticket ticket,
            NotificationType type,
            String title,
            String message
    ) {
        if (recipient == null) {
            log.debug("Skipping notification creation: recipient is null for type {}", type);
            return null;
        }

        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setTicket(ticket);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRead(false);

        Notification saved = notificationRepository.save(notification);

        try {
            emailService.sendNotificationEmail(recipient, title, message);
        } catch (Exception ex) {
            log.error("Failed to trigger notification email for recipient {}: {}", recipient.getId(), ex.getMessage(), ex);
        }

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(Pageable pageable) {
        long currentEmployeeId = currentUserService.getCurrentEmployeeId();
        Page<Notification> notifications = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(currentEmployeeId, pageable);
        return notifications.map(notificationMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUnreadNotifications(Pageable pageable) {
        long currentEmployeeId = currentUserService.getCurrentEmployeeId();
        Page<Notification> notifications = notificationRepository.findByRecipientIdAndReadFalseOrderByCreatedAtDesc(currentEmployeeId, pageable);
        return notifications.map(notificationMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadNotificationCountResponse getUnreadCount() {
        long currentEmployeeId = currentUserService.getCurrentEmployeeId();
        long count = notificationRepository.countByRecipientIdAndReadFalse(currentEmployeeId);
        return new UnreadNotificationCountResponse(count);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(Long notificationId) {
        long currentEmployeeId = currentUserService.getCurrentEmployeeId();

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));

        if (!notification.getRecipient().getId().equals(currentEmployeeId)) {
            throw new AccessDeniedException("You cannot access notifications belonging to another employee");
        }

        notification.setRead(true);
        notificationRepository.save(notification);

        return notificationMapper.toResponse(notification);
    }
}
