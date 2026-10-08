package xyz.mobi.employeehelpdesk.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import xyz.mobi.employeehelpdesk.dto.notification.NotificationResponseDTO;
import xyz.mobi.employeehelpdesk.dto.notification.NotificationUpdateResponseDTO;
import xyz.mobi.employeehelpdesk.dto.notification.UnreadNotificationCountResponseDTO;
import xyz.mobi.employeehelpdesk.entity.enums.NotificationType;

public interface NotificationService {

    void sendNotification(
            Long recipient,
            Long ticket,
            NotificationType type,
            String title,
            String message
    );

    Page<NotificationResponseDTO> getNotifications(Pageable pageable);

    Page<NotificationResponseDTO> getUnreadNotifications(Pageable pageable);

    UnreadNotificationCountResponseDTO getUnreadCount();

    NotificationUpdateResponseDTO markAsRead(Long notificationId);
}
