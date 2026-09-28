package com.example.helpdesk.service;

import com.example.helpdesk.dto.response.NotificationResponse;
import com.example.helpdesk.entity.Employee;
import com.example.helpdesk.entity.Ticket;
import com.example.helpdesk.enums.NotificationType;

import java.util.List;

public interface NotificationService {

    void sendNotification(Employee recipient, Ticket ticket, NotificationType type, String title, String message);

    void markAsRead(Long notificationId);

    void markAllAsReadForEmployee(Long employeeId);

    List<NotificationResponse> getNotificationsByEmployee(Long employeeId);
}
