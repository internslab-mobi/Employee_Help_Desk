package com.example.helpdesk.service;

<<<<<<< HEAD
=======
import com.example.helpdesk.dto.response.NotificationResponseDTO;
>>>>>>> c5b2367e (updated reworks)
import com.example.helpdesk.entity.Employee;
import com.example.helpdesk.entity.Ticket;
import com.example.helpdesk.enums.NotificationType;

public interface NotificationService {

    void sendNotification(Employee recipient, Ticket ticket, NotificationType type, String title, String message);

    void markAsRead(Long notificationId);

    void markAllAsReadForEmployee(Long employeeId);
<<<<<<< HEAD
=======

    List<NotificationResponseDTO> getNotificationsByEmployee(Long employeeId);
>>>>>>> c5b2367e (updated reworks)
}




