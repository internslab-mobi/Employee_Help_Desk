package com.divya.helpdesk.service;

import com.divya.helpdesk.dto.notification.NotificationResponseDTO;
import com.divya.helpdesk.entity.HDEmployeeEntity;
import com.divya.helpdesk.entity.HDNotificationEntity;
import com.divya.helpdesk.entity.HDTicketEntity;
import com.divya.helpdesk.enums.NotificationType;

import java.util.List;

public interface NotificationService {

    HDNotificationEntity createNotification(HDEmployeeEntity recipient, String title, String message, NotificationType type, HDTicketEntity referenceTicket);

    List<NotificationResponseDTO> getMyNotifications(Long employeeId);
}
