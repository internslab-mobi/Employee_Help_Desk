package xyz.mobi.notificationservice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import xyz.mobi.notificationservice.dto.CreateNotificationRequestDTO;
import xyz.mobi.notificationservice.dto.NotificationResponseDTO;
import xyz.mobi.notificationservice.dto.UnreadNotificationCountResponseDTO;

public interface NotificationService {

    NotificationResponseDTO createNotification(CreateNotificationRequestDTO request);

    Page<NotificationResponseDTO> getNotifications(Long recipientId, Pageable pageable);

    Page<NotificationResponseDTO> getUnreadNotifications(Long recipientId, Pageable pageable);

    UnreadNotificationCountResponseDTO getUnreadCount(Long recipientId);

    NotificationResponseDTO markAsRead(Long notificationId);
}
