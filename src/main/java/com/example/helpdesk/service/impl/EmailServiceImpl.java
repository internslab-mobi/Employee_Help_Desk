package com.example.helpdesk.service.impl;

import com.example.helpdesk.entity.Employee;
import com.example.helpdesk.entity.Ticket;
import com.example.helpdesk.service.EmailService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.enabled:true}")
    private boolean emailEnabled;

    @Value("${spring.mail.username}")
    private String mailUsername;

    @Value("${spring.mail.password}")
    private String mailPassword;

    @Value("${spring.mail.host:smtp.gmail.com}")
    private String mailHost;

    private static final int MAX_RETRY_ATTEMPTS = 3;

    @PostConstruct
    public void validateMailConfiguration() {
        if (emailEnabled) {
            if (mailUsername == null || mailUsername.trim().isEmpty()) {
                log.error("Mail configuration is missing: SMTP_USERNAME is not configured. Email sending will be disabled.");
                emailEnabled = false;
            } else if (mailPassword == null || mailPassword.trim().isEmpty()) {
                log.error("Mail configuration is missing: SMTP_PASSWORD is not configured. Email sending will be disabled.");
                emailEnabled = false;
            } else {
                log.info("Mail configuration validated successfully. Host: {}, Username: {}", mailHost, maskEmail(mailUsername));
            }
        } else {
            log.info("Email sending is disabled via configuration.");
        }
    }

    private String maskEmail(String email) {
        if (email == null || email.length() < 3) {
            return "***";
        }
        int atIndex = email.indexOf('@');
        if (atIndex > 0) {
            return email.charAt(0) + "***" + email.substring(atIndex);
        }
        return "***";
    }

    private boolean isValidRecipient(String email) {
        return email != null && !email.trim().isEmpty() && email.contains("@");
    }

    @Override
    @Async("emailTaskExecutor")
    public void sendTicketCreatedEmail(Employee recipient, Ticket ticket) {
        if (!emailEnabled) {
            log.debug("Email sending disabled");
            return;
        }

        if (!isValidRecipient(recipient.getEmail())) {
            log.warn("Invalid recipient email for ticket creation email: {}", recipient.getEmail());
            return;
        }

        sendEmailWithRetry(recipient.getEmail(), 
                "New Ticket Created: " + ticket.getTicketNumber(),
                buildTicketCreatedEmailBody(ticket),
                "ticket creation");
    }

    @Override
    @Async("emailTaskExecutor")
    public void sendTicketHoldEmail(Employee requester, Ticket ticket, String holdReason) {
        if (!emailEnabled) {
            log.debug("Email sending disabled");
            return;
        }

        if (!isValidRecipient(requester.getEmail())) {
            log.warn("Invalid recipient email for ticket hold email: {}", requester.getEmail());
            return;
        }

        sendEmailWithRetry(requester.getEmail(),
                "Ticket Requires Your Attention: " + ticket.getTicketNumber(),
                buildTicketHoldEmailBody(ticket, holdReason),
                "ticket hold");
    }

    @Override
    @Async("emailTaskExecutor")
    public void sendTicketResolvedEmail(Employee requester, Ticket ticket, String resolutionSummary) {
        if (!emailEnabled) {
            log.debug("Email sending disabled");
            return;
        }

        if (!isValidRecipient(requester.getEmail())) {
            log.warn("Invalid recipient email for ticket resolved email: {}", requester.getEmail());
            return;
        }

        sendEmailWithRetry(requester.getEmail(),
                "Ticket Resolved: " + ticket.getTicketNumber(),
                buildTicketResolvedEmailBody(ticket, resolutionSummary),
                "ticket resolution");
    }

    @Override
    @Async("emailTaskExecutor")
    public void sendTicketReopenedEmail(Employee recipient, Ticket ticket) {
        if (!emailEnabled) {
            log.debug("Email sending disabled");
            return;
        }

        if (!isValidRecipient(recipient.getEmail())) {
            log.warn("Invalid recipient email for ticket reopened email: {}", recipient.getEmail());
            return;
        }

        sendEmailWithRetry(recipient.getEmail(),
                "Ticket Reopened: " + ticket.getTicketNumber(),
                buildTicketReopenedEmailBody(ticket),
                "ticket reopen");
    }

    @Override
    @Async("emailTaskExecutor")
    public void sendTicketWithdrawnEmail(Employee requester, Ticket ticket, String withdrawalReason) {
        if (!emailEnabled) {
            log.debug("Email sending disabled");
            return;
        }

        if (!isValidRecipient(requester.getEmail())) {
            log.warn("Invalid recipient email for ticket withdrawn email: {}", requester.getEmail());
            return;
        }

        sendEmailWithRetry(requester.getEmail(),
                "Ticket Withdrawn: " + ticket.getTicketNumber(),
                buildTicketWithdrawnEmailBody(ticket, withdrawalReason),
                "ticket withdrawal");
    }

    @Override
    public void sendAccountCreatedEmail(Employee employee, String temporaryPassword, String otp, int otpExpirationMinutes) {
        if (!emailEnabled) {
            log.debug("Email sending disabled");
            return;
        }

        if (!isValidRecipient(employee.getEmail())) {
            log.warn("Invalid recipient email for account creation email: {}", employee.getEmail());
            return;
        }

        // OTP email is security-critical - send synchronously to ensure delivery before returning
        sendEmailWithRetry(employee.getEmail(),
                "Employee Help Desk - Account Created",
                buildAccountCreatedEmailBody(employee, temporaryPassword, otp, otpExpirationMinutes),
                "account creation");
    }

    private void sendEmailWithRetry(String to, String subject, String body, String emailType) {
        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(mailUsername);
                message.setTo(to);
                message.setSubject(subject);
                message.setText(body);
                
                mailSender.send(message);
                log.info("Successfully sent {} email to {} (attempt {})", emailType, maskEmail(to), attempt);
                return;
            } catch (Exception e) {
                lastException = e;
                log.warn("Failed to send {} email to {} on attempt {}/{}: {}", 
                        emailType, maskEmail(to), attempt, MAX_RETRY_ATTEMPTS, e.getMessage());
                
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    try {
                        Thread.sleep(1000 * attempt); // Exponential backoff: 1s, 2s, 3s
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("Thread interrupted during email retry backoff");
                        break;
                    }
                }
            }
        }
        
        log.error("Failed to send {} email to {} after {} attempts: {}", 
                emailType, maskEmail(to), MAX_RETRY_ATTEMPTS, lastException != null ? lastException.getMessage() : "unknown");
    }

    private String buildTicketCreatedEmailBody(Ticket ticket) {
        StringBuilder sb = new StringBuilder();
        sb.append("Dear ").append(ticket.getRequester().getFirstName()).append(",\n\n");
        sb.append("Your ticket has been successfully created and assigned.\n\n");
        sb.append("Ticket Number: ").append(ticket.getTicketNumber()).append("\n");
        sb.append("Subject: ").append(ticket.getSubject()).append("\n");
        sb.append("Description: ").append(ticket.getDescription()).append("\n");
        sb.append("Priority: ").append(ticket.getPriority()).append("\n");
        sb.append("Category: ").append(ticket.getCategory() != null ? ticket.getCategory().getName() : "N/A").append("\n");
        sb.append("Department: ").append(ticket.getDepartment().getName()).append("\n");
        sb.append("Status: ").append(ticket.getStatus()).append("\n\n");
        sb.append("We will work on your ticket and keep you updated.\n\n");
        sb.append("Thank you,\nHelp Desk Team");
        return sb.toString();
    }

    private String buildTicketHoldEmailBody(Ticket ticket, String holdReason) {
        StringBuilder sb = new StringBuilder();
        sb.append("Dear ").append(ticket.getRequester().getFirstName()).append(",\n\n");
        sb.append("Your ticket requires your communication/action to proceed.\n\n");
        sb.append("Ticket Number: ").append(ticket.getTicketNumber()).append("\n");
        sb.append("Subject: ").append(ticket.getSubject()).append("\n");
        sb.append("Current Status: ").append(ticket.getStatus()).append("\n");
        if (holdReason != null && !holdReason.isEmpty()) {
            sb.append("Reason: ").append(holdReason).append("\n");
        }
        sb.append("\nPlease respond to the ticket so we can continue working on it.\n\n");
        sb.append("Thank you,\nHelp Desk Team");
        return sb.toString();
    }

    private String buildTicketResolvedEmailBody(Ticket ticket, String resolutionSummary) {
        StringBuilder sb = new StringBuilder();
        sb.append("Dear ").append(ticket.getRequester().getFirstName()).append(",\n\n");
        sb.append("Your ticket has been resolved.\n\n");
        sb.append("Ticket Number: ").append(ticket.getTicketNumber()).append("\n");
        sb.append("Subject: ").append(ticket.getSubject()).append("\n");
        if (resolutionSummary != null && !resolutionSummary.isEmpty()) {
            sb.append("Resolution: ").append(resolutionSummary).append("\n");
        }
        sb.append("Resolved At: ").append(ticket.getResolvedAt()).append("\n");
        sb.append("Status: ").append(ticket.getStatus()).append("\n\n");
        sb.append("Please provide feedback on your experience.\n\n");
        sb.append("Thank you,\nHelp Desk Team");
        return sb.toString();
    }

    private String buildTicketReopenedEmailBody(Ticket ticket) {
        StringBuilder sb = new StringBuilder();
        sb.append("Dear ").append(ticket.getAssignedAgent() != null ? 
                ticket.getAssignedAgent().getEmployee().getFirstName() : "Team Member").append(",\n\n");
        sb.append("A ticket has been reopened and a new SLA cycle has started.\n\n");
        sb.append("Ticket Number: ").append(ticket.getTicketNumber()).append("\n");
        sb.append("Subject: ").append(ticket.getSubject()).append("\n");
        sb.append("Reopened At: ").append(ticket.getReopenedAt()).append("\n");
        sb.append("Reopen Count: ").append(ticket.getReopenCount()).append("\n");
        sb.append("Status: ").append(ticket.getStatus()).append("\n\n");
        sb.append("Please work on this ticket according to the new SLA.\n\n");
        sb.append("Thank you,\nHelp Desk Team");
        return sb.toString();
    }

    private String buildTicketWithdrawnEmailBody(Ticket ticket, String withdrawalReason) {
        StringBuilder sb = new StringBuilder();
        sb.append("Dear ").append(ticket.getRequester().getFirstName()).append(",\n\n");
        sb.append("Your ticket has been withdrawn.\n\n");
        sb.append("Ticket Number: ").append(ticket.getTicketNumber()).append("\n");
        sb.append("Subject: ").append(ticket.getSubject()).append("\n");
        if (withdrawalReason != null && !withdrawalReason.isEmpty()) {
            sb.append("Withdrawal Reason: ").append(withdrawalReason).append("\n");
        }
        sb.append("Withdrawn At: ").append(ticket.getWithdrawnAt()).append("\n");
        sb.append("Status: ").append(ticket.getStatus()).append("\n\n");
        sb.append("The ticket history and attachments remain available.\n\n");
        sb.append("Thank you,\nHelp Desk Team");
        return sb.toString();
    }

    private String buildAccountCreatedEmailBody(Employee employee, String temporaryPassword, String otp, int otpExpirationMinutes) {
        StringBuilder sb = new StringBuilder();
        sb.append("Hello ").append(employee.getFirstName()).append(",\n\n");
        sb.append("Your Employee Help Desk account has been created by the administrator.\n\n");
        sb.append("Login Email:\n").append(employee.getEmail()).append("\n\n");
        sb.append("Temporary Password:\n").append(temporaryPassword).append("\n\n");
        sb.append("One-Time Password (OTP):\n").append(otp).append("\n\n");
        sb.append("The OTP is valid for ").append(otpExpirationMinutes).append(" minutes.\n\n");
        sb.append("Please log in using the temporary password and OTP.\n\n");
        sb.append("For security, you will be required to create your own password after your first successful login.\n\n");
        sb.append("Regards,\nEmployee Help Desk");
        return sb.toString();
    }
}
