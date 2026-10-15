package xyz.mobi.employeehelpdesk.service.helperservice;

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

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${helpdesk.mail.from:helpdesk@example.com}")
    private String fromAddress;

    @Async
    public void sendNotificationEmail(
            String recipientEmail,
            String subject,
            String message
    ) {

        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.warn(
                    "Cannot send notification email: recipient email is null/empty"
            );
            return;
        }

        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();

            mailMessage.setFrom(fromAddress);
            mailMessage.setTo(recipientEmail);
            mailMessage.setSubject(subject);
            mailMessage.setText(message);

            mailSender.send(mailMessage);

            log.info(
                    "Notification email sent successfully to {}",
                    recipientEmail
            );

        } catch (Exception ex) {
            log.error(
                    "Failed to send notification email to {}: {}",
                    recipientEmail,
                    ex.getMessage(),
                    ex
            );
        }
    }

    @Async
    public void sendReportEmailWithAttachment(
            String recipientEmail,
            String recipientName,
            String subject,
            String body,
            String attachmentFilename,
            byte[] attachmentData
    ) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.warn("Cannot send report email: recipient email is null/empty");
            return;
        }

        if (attachmentData == null || attachmentData.length == 0) {
            log.warn("Cannot send report email: attachment data is empty for recipient {}", recipientEmail);
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(body, false);

            String filename = (attachmentFilename != null && !attachmentFilename.isBlank())
                    ? attachmentFilename
                    : "Helpdesk_Report.xlsx";

            helper.addAttachment(
                    filename,
                    new ByteArrayResource(attachmentData),
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            );

            mailSender.send(mimeMessage);

            log.info("Report email with attachment '{}' sent successfully to {}", filename, recipientEmail);

        } catch (Exception ex) {
            log.error("Failed to send report email with attachment to {}: {}", recipientEmail, ex.getMessage(), ex);
        }
    }
}