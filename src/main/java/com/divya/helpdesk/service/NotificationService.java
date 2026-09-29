package com.divya.helpdesk.service;

import com.divya.helpdesk.dto.notification.NotificationResponse;
import com.divya.helpdesk.entity.HDEmployee;
import com.divya.helpdesk.entity.HDNotification;
import com.divya.helpdesk.entity.HDTicket;
import com.divya.helpdesk.enums.NotificationType;

import java.util.List;

public interface NotificationService {

    HDNotification createNotification(HDEmployee recipient, String title, String message, NotificationType type, HDTicket referenceTicket);

    List<NotificationResponse> getMyNotifications(Long employeeId);
}
