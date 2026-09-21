package xyz.mobi.employeehelpdesk.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import xyz.mobi.employeehelpdesk.dto.notification.NotificationResponse;
import xyz.mobi.employeehelpdesk.dto.notification.UnreadNotificationCountResponse;
import xyz.mobi.employeehelpdesk.entity.Employee;
import xyz.mobi.employeehelpdesk.entity.Notification;
import xyz.mobi.employeehelpdesk.entity.Ticket;
import xyz.mobi.employeehelpdesk.entity.enums.NotificationType;

public interface NotificationService {

    Notification sendNotification(
            Employee recipient,
            Ticket ticket,
            NotificationType type,
            String title,
            String message
    );

    Page<NotificationResponse> getNotifications(Pageable pageable);

    Page<NotificationResponse> getUnreadNotifications(Pageable pageable);

    UnreadNotificationCountResponse getUnreadCount();

    NotificationResponse markAsRead(Long notificationId);
}
