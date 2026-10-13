package xyz.mobi.notificationservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.mobi.notificationservice.dto.CreateNotificationRequestDTO;
import xyz.mobi.notificationservice.dto.NotificationResponseDTO;
import xyz.mobi.notificationservice.dto.UnreadNotificationCountResponseDTO;
import xyz.mobi.notificationservice.entity.Notification;
import xyz.mobi.notificationservice.exception.BadRequestException;
import xyz.mobi.notificationservice.exception.ResourceNotFoundException;
import xyz.mobi.notificationservice.mapper.NotificationMapper;
import xyz.mobi.notificationservice.repository.NotificationRepository;
import xyz.mobi.notificationservice.service.EmailService;
import xyz.mobi.notificationservice.service.NotificationService;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final EmailService emailService;

    @Override
    @Transactional
    public NotificationResponseDTO createNotification(CreateNotificationRequestDTO request) {
        if (request == null) {
            throw new BadRequestException("Notification request cannot be null");
        }
        if (request.recipientId() == null) {
            throw new BadRequestException("Recipient ID is required");
        }

        Notification notification = notificationMapper.toEntity(request);
        Notification saved = notificationRepository.save(notification);

        log.info("Notification created successfully: id={}, recipientId={}, ticketId={}, type={}",
                saved.getId(), saved.getRecipientId(), saved.getTicketId(), saved.getType());

        String recipientEmail = request.recipientEmail();
        if (recipientEmail != null && !recipientEmail.isBlank()) {
            emailService.sendNotificationEmail(recipientEmail, request.title(), request.message());
        } else {
            log.warn("Skipping notification email: recipient {} has no email address", request.recipientId());
        }

        return notificationMapper.toCreateResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponseDTO> getNotifications(Long recipientId, Pageable pageable) {
        Page<Notification> notifications;
        if (recipientId != null) {
            notifications = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId, pageable);
        } else {
            notifications = notificationRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return notifications.map(notificationMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponseDTO> getUnreadNotifications(Long recipientId, Pageable pageable) {
        Page<Notification> notifications;
        if (recipientId != null) {
            notifications = notificationRepository.findByRecipientIdAndReadFalseOrderByCreatedAtDesc(recipientId, pageable);
        } else {
            notifications = notificationRepository.findByReadFalseOrderByCreatedAtDesc(pageable);
        }
        return notifications.map(notificationMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadNotificationCountResponseDTO getUnreadCount(Long recipientId) {
        Long count;
        if (recipientId != null) {
            count = notificationRepository.countByRecipientIdAndReadFalse(recipientId);
        } else {
            count = notificationRepository.countByReadFalse();
        }
        return new UnreadNotificationCountResponseDTO(count != null ? count : 0L);
    }

    @Override
    @Transactional
    public NotificationResponseDTO markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));

        notification.setRead(true);
        Notification updated = notificationRepository.save(notification);

        log.info("Notification marked as read: notificationId={}, recipientId={}",
                notificationId, updated.getRecipientId());

        return notificationMapper.toUpdateResponse(updated);
    }
}
