package xyz.mobi.notificationservice.service;

import xyz.mobi.notificationservice.dto.EmailRequestDTO;

public interface EmailService {

    void sendNotificationEmail(String recipientEmail, String subject, String message);

    void sendEmail(EmailRequestDTO request);
}
