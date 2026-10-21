package xyz.mobi.notificationservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import xyz.mobi.notificationservice.dto.EmailRequestDTO;
import xyz.mobi.notificationservice.service.EmailService;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${helpdesk.mail.from:helpdesk@example.com}")
    private String fromAddress;

    @Override
    @Async
    public void sendNotificationEmail(
            String recipientEmail,
            String subject,
            String message
    ) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.warn("Cannot send notification email: recipient email is null/empty");
            return;
        }

        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();

            mailMessage.setFrom(fromAddress);
            mailMessage.setTo(recipientEmail);
            mailMessage.setSubject(subject);
            mailMessage.setText(message);

            mailSender.send(mailMessage);

            log.info("Notification email sent successfully to {}", recipientEmail);

        } catch (Exception ex) {
            log.error("Failed to send notification email to {}: {}",
                    recipientEmail, ex.getMessage(), ex);
        }
    }

    @Override
    public void sendEmail(EmailRequestDTO request) {
        if (request == null) {
            log.warn("Cannot send email: request is null");
            return;
        }
        sendNotificationEmail(request.to(), request.subject(), request.body());
    }
}
