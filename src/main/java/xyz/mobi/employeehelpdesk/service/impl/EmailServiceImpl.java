package xyz.mobi.employeehelpdesk.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import xyz.mobi.employeehelpdesk.entity.Employee;
import xyz.mobi.employeehelpdesk.service.EmailService;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${helpdesk.mail.from:helpdesk@example.com}")
    private String fromAddress;

    @Override
    public void sendNotificationEmail(Employee recipient, String subject, String message) {
        if (recipient == null || recipient.getEmail() == null || recipient.getEmail().isBlank()) {
            log.warn("Cannot send notification email: recipient or recipient email is null/empty");
            return;
        }

        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(fromAddress);
            mailMessage.setTo(recipient.getEmail());
            mailMessage.setSubject(subject);
            mailMessage.setText(message);

            mailSender.send(mailMessage);
            log.info("Notification email sent successfully to {}", recipient.getEmail());
        } catch (Exception ex) {
            log.error("Failed to send notification email to {}: {}", recipient.getEmail(), ex.getMessage(), ex);
            // External side effect: catch and log to prevent roll back of caller transaction
        }
    }
}
