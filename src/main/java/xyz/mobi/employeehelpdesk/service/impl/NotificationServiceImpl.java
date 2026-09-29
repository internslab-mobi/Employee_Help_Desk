package xyz.mobi.employeehelpdesk.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.mobi.employeehelpdesk.dto.notification.NotificationResponse;
import xyz.mobi.employeehelpdesk.dto.notification.NotificationUpdateResponse;
import xyz.mobi.employeehelpdesk.dto.notification.UnreadNotificationCountResponse;
import xyz.mobi.employeehelpdesk.entity.Employee;
import xyz.mobi.employeehelpdesk.entity.Notification;
import xyz.mobi.employeehelpdesk.entity.Ticket;
import xyz.mobi.employeehelpdesk.entity.enums.NotificationType;
import xyz.mobi.employeehelpdesk.exception.ResourceNotFoundException;
import xyz.mobi.employeehelpdesk.mapper.NotificationMapper;
import xyz.mobi.employeehelpdesk.repository.EmployeeRepository;
import xyz.mobi.employeehelpdesk.repository.NotificationRepository;
import xyz.mobi.employeehelpdesk.repository.TicketRepository;
import xyz.mobi.employeehelpdesk.service.AuthService;
import xyz.mobi.employeehelpdesk.service.NotificationService;
import xyz.mobi.employeehelpdesk.service.helperservice.EmailService;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final EmailService emailService;
    private final AuthService authService;
    private final EmployeeRepository employeeRepository;
    private final TicketRepository ticketRepository;

    @Override
    @Transactional
    public void sendNotification(
            Long recipientId,
            Long ticketId,
            NotificationType type,
            String title,
            String message
    ) {

        if (recipientId == null) {
            log.debug(
                    "Skipping notification creation: recipientId is null for type {}",
                    type
            );
            return;
        }
        Employee recipient = employeeRepository.findById(recipientId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Employee not found: " + recipientId
                        )
                );

        Ticket ticket = null;

        if (ticketId != null) {
            ticket = ticketRepository.findById(ticketId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Ticket not found: " + ticketId
                            )
                    );
        }

        Notification notification = new Notification();

        notification.setRecipient(recipient);
        notification.setTicket(ticket);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRead(false);

        notificationRepository.save(notification);

        String recipientEmail = recipient.getEmail();

        if (recipientEmail != null && !recipientEmail.isBlank()) {

            emailService.sendNotificationEmail(
                    recipientEmail,
                    title,
                    message
            );

        } else {

            log.warn(
                    "Skipping notification email: employee {} has no email address",
                    recipientId
            );
        }
    }
    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(Pageable pageable) {
        long currentEmployeeId = authService.getCurrentEmployeeId();
        Page<Notification> notifications = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(currentEmployeeId, pageable);
        return notifications.map(notificationMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUnreadNotifications(Pageable pageable) {
        long currentEmployeeId = authService.getCurrentEmployeeId();
        Page<Notification> notifications = notificationRepository.findByRecipientIdAndReadFalseOrderByCreatedAtDesc(currentEmployeeId, pageable);
        return notifications.map(notificationMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadNotificationCountResponse getUnreadCount() {
        long currentEmployeeId = authService.getCurrentEmployeeId();
        long count = notificationRepository.countByRecipientIdAndReadFalse(currentEmployeeId);
        return new UnreadNotificationCountResponse(count);
    }

    @Override
    @Transactional
    public NotificationUpdateResponse markAsRead(Long notificationId) {
        long currentEmployeeId = authService.getCurrentEmployeeId();

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));

        if (!notification.getRecipient().getId().equals(currentEmployeeId)) {
            throw new AccessDeniedException("You cannot access notifications belonging to another employee");
        }

        notification.setRead(true);
        notificationRepository.save(notification);

        return notificationMapper.toUpdateResponse(notification);
    }
}
