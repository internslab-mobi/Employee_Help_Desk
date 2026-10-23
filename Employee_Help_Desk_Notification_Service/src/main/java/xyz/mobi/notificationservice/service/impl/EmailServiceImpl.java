package xyz.mobi.notificationservice.service.impl;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import xyz.mobi.notificationservice.dto.EmailRequestDTO;
import xyz.mobi.notificationservice.service.EmailService;

import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${helpdesk.mail.from:helpdesk@example.com}")
    private String fromAddress;

    @Override
    @Async
    public void sendEmail(EmailRequestDTO request) {
        if (request == null) {
            log.warn("Cannot send email: request is null");
            return;
        }

        String recipientEmail = request.to();
        String subject = request.subject();
        String message = request.body();

        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.warn("Cannot send notification email: recipient email is null/empty");
            return;
        }

        try {
            boolean hasAttachment = request.attachmentBase64() != null && !request.attachmentBase64().isBlank();

            if (hasAttachment) {
                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
                helper.setFrom(fromAddress);
                helper.setTo(recipientEmail);
                helper.setSubject(subject);
                helper.setText(message, false);

                String filename = (request.attachmentFilename() != null && !request.attachmentFilename().isBlank())
                        ? request.attachmentFilename()
                        : "attachment.bin";

                byte[] data = Base64.getDecoder().decode(request.attachmentBase64());
                helper.addAttachment(filename, new ByteArrayResource(data));
                mailSender.send(mimeMessage);
                log.info("Email with attachment '{}' sent successfully to {}", filename, recipientEmail);
            } else {
                SimpleMailMessage mailMessage = new SimpleMailMessage();
                mailMessage.setFrom(fromAddress);
                mailMessage.setTo(recipientEmail);
                mailMessage.setSubject(subject);
                mailMessage.setText(message);
                mailSender.send(mailMessage);
                log.info("Notification email sent successfully to {}", recipientEmail);
            }
        } catch (Exception ex) {
            log.error("Failed to send notification email to {}: {}",
                    recipientEmail, ex.getMessage(), ex);
        }
    }
}
