package com.divya.helpdesk.service.impl;

import com.divya.helpdesk.dto.notification.NotificationResponseDTO;
import com.divya.helpdesk.dto.notification.ReferenceTicketDTO;
import com.divya.helpdesk.entity.HDEmployeeEntity;
import com.divya.helpdesk.entity.HDNotificationEntity;
import com.divya.helpdesk.entity.HDTicketEntity;
import com.divya.helpdesk.enums.NotificationType;
import com.divya.helpdesk.repository.HDNotificationRepository;
import com.divya.helpdesk.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final HDNotificationRepository notificationRepository;

    @Override
    public HDNotificationEntity createNotification(HDEmployeeEntity recipient, String title, String message, NotificationType type, HDTicketEntity referenceTicket) {
        if (recipient == null) {
            log.warn("Cannot create notification with null recipient for ticket: {}",
                    referenceTicket != null ? referenceTicket.getTicketNumber() : "N/A");
            return null;
        }

        HDNotificationEntity notification = new HDNotificationEntity();
        notification.setRecipient(recipient);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setReferenceTicket(referenceTicket);

        log.info("Created in-app notification [{}] for recipient: {}", type, recipient.getEmail());
        return notificationRepository.save(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponseDTO> getMyNotifications(Long employeeId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(employeeId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private NotificationResponseDTO mapToResponse(HDNotificationEntity notification) {
        return NotificationResponseDTO.builder()
                .id(notification.getId())
                .recipientId(notification.getRecipient() != null ? notification.getRecipient().getId() : null)
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .reference(mapToReferenceTicket(notification.getReferenceTicket()))
                .createdAt(notification.getCreatedAt().atZone(ZoneId.of(notification.getRecipient().getTimezone())).toOffsetDateTime())
                .build();
    }

    private ReferenceTicketDTO mapToReferenceTicket(HDTicketEntity ticket){
        if(ticket == null) return null;

        return ReferenceTicketDTO.builder()
                .id(ticket.getId())
                .ticketNumber(ticket.getTicketNumber())
                .build();
    }
}
