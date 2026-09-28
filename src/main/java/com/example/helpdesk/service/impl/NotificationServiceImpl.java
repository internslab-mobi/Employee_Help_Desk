package com.example.helpdesk.service.impl;

import com.example.helpdesk.dto.response.NotificationResponse;
import com.example.helpdesk.entity.Employee;
import com.example.helpdesk.entity.Notification;
import com.example.helpdesk.entity.Ticket;
import com.example.helpdesk.enums.NotificationType;
import com.example.helpdesk.exception.AuthorizationException;
import com.example.helpdesk.repository.NotificationRepository;
import com.example.helpdesk.service.EmailService;
import com.example.helpdesk.util.AuthenticatedEmployeeUtil;
import com.example.helpdesk.util.TimezoneUtil;
import com.example.helpdesk.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final AuthenticatedEmployeeUtil authenticatedEmployeeUtil;

    private Long getAuthenticatedEmployeeId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new AuthorizationException("User not authenticated");
        }
        try {
            return Long.parseLong(authentication.getPrincipal().toString());
        } catch (NumberFormatException e) {
            throw new AuthorizationException("Invalid employee ID in authentication context");
        }
    }

    private String getAuthenticatedRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            throw new AuthorizationException("User not authenticated");
        }
        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(auth -> auth.startsWith("ROLE_"))
                .map(auth -> auth.substring(5))
                .findFirst()
                .orElseThrow(() -> new AuthorizationException("No role found in authentication context"));
    }

    @Override
    @Transactional
    public void sendNotification(Employee recipient, Ticket ticket, NotificationType type, String title, String message) {
        // Save database notification
        Notification notification = Notification.builder()
                .recipient(recipient)
                .ticket(ticket)
                .type(type != null ? type.name() : null)
                .title(title)
                .message(message)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        notificationRepository.save(notification);
        log.info("Sent notification to employee {} for ticket {}: type={}",
                recipient.getId(), ticket != null ? ticket.getId() : "N/A", type);

        // Send email for specific notification types
        if (ticket != null && type != null) {
            switch (type) {
                case TICKET_CREATED:
                    emailService.sendTicketCreatedEmail(recipient, ticket);
                    break;
                case TICKET_HOLD:
                    emailService.sendTicketHoldEmail(ticket.getRequester(), ticket, ticket.getHoldReason());
                    break;
                case TICKET_RESOLVED:
                    emailService.sendTicketResolvedEmail(ticket.getRequester(), ticket, ticket.getResolutionSummary());
                    break;
                case TICKET_REOPENED:
                    emailService.sendTicketReopenedEmail(recipient, ticket);
                    break;
                case TICKET_WITHDRAWN:
                    emailService.sendTicketWithdrawnEmail(ticket.getRequester(), ticket, ticket.getWithdrawalReason());
                    break;
                default:
                    // No email for other notification types
                    break;
            }
        }
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId) {
        Long employeeId = getAuthenticatedEmployeeId();
        String role = getAuthenticatedRole();

        if (!"ADMIN".equals(role)) {
            if (!notificationRepository.existsByIdAndRecipientId(notificationId, employeeId)) {
                throw new AuthorizationException("You can only mark your own notifications as read");
            }
        }

        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setReadAt(Instant.now());
            notification.setUpdatedAt(Instant.now());
            notificationRepository.save(notification);
            log.info("Marked notification {} as read", notificationId);
        });
    }

    @Override
    @Transactional
    public void markAllAsReadForEmployee(Long employeeId) {
        Long authenticatedEmployeeId = getAuthenticatedEmployeeId();
        String role = getAuthenticatedRole();

        if (!"ADMIN".equals(role)) {
            if (!authenticatedEmployeeId.equals(employeeId)) {
                throw new AuthorizationException("You can only mark your own notifications as read");
            }
        }

        List<Notification> unreadNotifications = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(employeeId);
        unreadNotifications.stream()
                .filter(n -> n.getReadAt() == null)
                .forEach(n -> {
                    n.setReadAt(Instant.now());
                    n.setUpdatedAt(Instant.now());
                    notificationRepository.save(n);
                });
        log.info("Marked all notifications as read for employee {}", employeeId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByEmployee(Long employeeId) {
        Long authenticatedEmployeeId = getAuthenticatedEmployeeId();
        String role = getAuthenticatedRole();

        if (!"ADMIN".equals(role)) {
            if (!authenticatedEmployeeId.equals(employeeId)) {
                throw new AuthorizationException("You can only view your own notifications");
            }
        }

        List<Notification> notifications = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(employeeId);
        return notifications.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private NotificationResponse toResponse(Notification notification) {
        String timezone = authenticatedEmployeeUtil.getAuthenticatedEmployeeTimezone();
        return NotificationResponse.builder()
                .id(notification.getId())
                .recipientId(notification.getRecipient().getId())
                .ticketId(notification.getTicket() != null ? notification.getTicket().getId() : null)
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .createdAt(TimezoneUtil.toOffsetDateTime(notification.getCreatedAt(), timezone))
                .readAt(notification.getReadAt() != null ? TimezoneUtil.toOffsetDateTime(notification.getReadAt(), timezone) : null)
                .build();
    }

}

