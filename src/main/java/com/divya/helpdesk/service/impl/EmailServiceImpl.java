package com.divya.helpdesk.service.impl;

import com.divya.helpdesk.entity.HDEmployee;
import com.divya.helpdesk.entity.HDTicket;
import com.divya.helpdesk.entity.HDTicketFeedback;
import com.divya.helpdesk.entity.HDTicketMessage;
import com.divya.helpdesk.enums.TicketStatus;
import com.divya.helpdesk.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${spring.mail.username:${MAIL_USERNAME:noreply@helpdesk.com}}")
    private String fromEmail;

    @Override
    public void sendOtpEmail(String toEmail, String otp) {
        String subject = "Help Desk - Password Reset OTP";
        String body = String.format("Hello,\n\nYour OTP for password reset is: %s\n\nThis OTP will expire in 5 minutes.\n\nRegards,\nEmployee Help Desk Team", otp);
        sendEmailAsync(toEmail, subject, body);
    }

    @Override
    public void sendTemporaryPasswordEmail(String toEmail, String employeeName, String employeeCode, String temporaryPassword) {
        String subject = "Your Help Desk Account Has Been Created";
        String body = String.format(
                "Hello %s,\n\n"
                        + "Welcome to the Employee Help Desk! Your account has been created.\n\n"
                        + "Employee Code: %s\n"
                        + "Name: %s\n"
                        + "Email: %s\n"
                        + "Temporary Password: %s\n\n"
                        + "Please use these credentials to activate your account and complete the account setup.\n"
                        + "For security reasons, do not share this temporary password with anyone.\n\n"
                        + "Regards,\nEmployee Help Desk Team",
                employeeName != null ? employeeName : "Employee",
                employeeCode != null ? employeeCode : "N/A",
                employeeName != null ? employeeName : "N/A",
                toEmail, temporaryPassword);
        sendEmailAsync(toEmail, subject, body);
    }

    @Override
    public void sendAccountActivatedEmail(String toEmail, String employeeName) {
        String subject = "Help Desk - Account Activated";
        String body = String.format(
                "Hello %s,\n\nYour Employee Help Desk account has been successfully activated. You can now log in.\n\nRegards,\nEmployee Help Desk Team",
                employeeName != null ? employeeName : "User"
        );
        sendEmailAsync(toEmail, subject, body);
    }

    @Override
    public void sendTicketCreatedEmail(HDTicket ticket) {
        if (ticket == null || ticket.getRequester() == null || ticket.getRequester().getEmail() == null) {
            return;
        }
        // Extract all values synchronously before async dispatch
        String toEmail = ticket.getRequester().getEmail();
        String requesterName = ticket.getRequester().getFirstName();
        String ticketNumber = ticket.getTicketNumber();
        String deptName = ticket.getDepartment() != null ? ticket.getDepartment().getName() : "N/A";
        String catName = ticket.getCategory() != null ? ticket.getCategory().getName() : "N/A";
        String subCatName = ticket.getSubCategory() != null ? ticket.getSubCategory().getName() : "N/A";
        String priority = ticket.getPriority() != null ? ticket.getPriority().name() : "MEDIUM";
        String status = ticket.getStatus() != null ? ticket.getStatus().name() : "NEW";
        String description = ticket.getDescription() != null ? ticket.getDescription() : "";

        String subject = String.format("Ticket Created - %s [%s]", ticketNumber, priority);
        String body = String.format(
                "Hello %s,\n\nYour ticket %s has been created successfully.\n\nDepartment: %s\nCategory: %s\nSub-Category: %s\nPriority: %s\nStatus: %s\n\nDescription:\n%s\n\nRegards,\nEmployee Help Desk Team",
                requesterName != null ? requesterName : "User",
                ticketNumber, deptName, catName, subCatName, priority, status, description);
        sendEmailAsync(toEmail, subject, body);
    }

    @Override
    public void sendTicketAssignedEmail(HDTicket ticket, HDEmployee agent) {
        if (ticket == null || agent == null || agent.getEmail() == null) {
            return;
        }
        // Extract all values synchronously
        String toEmail = agent.getEmail();
        String agentName = agent.getFirstName();
        String ticketNumber = ticket.getTicketNumber();
        String reqFirstName = ticket.getRequester() != null ? ticket.getRequester().getFirstName() : "";
        String reqLastName = ticket.getRequester() != null ? ticket.getRequester().getLastName() : "";
        String deptName = ticket.getDepartment() != null ? ticket.getDepartment().getName() : "N/A";
        String catName = ticket.getCategory() != null ? ticket.getCategory().getName() : "N/A";
        String subCatName = ticket.getSubCategory() != null ? ticket.getSubCategory().getName() : "N/A";
        String priority = ticket.getPriority() != null ? ticket.getPriority().name() : "MEDIUM";
        String description = ticket.getDescription() != null ? ticket.getDescription() : "";

        String subject = String.format("Ticket Assigned to You - %s [%s]", ticketNumber, priority);
        String body = String.format(
                "Hello %s,\n\nYou have been assigned to ticket %s.\n\nRequester: %s %s\nDepartment: %s\nCategory: %s\nSub-Category: %s\nPriority: %s\n\nDescription:\n%s\n\nPlease review and attend to it according to SLA requirements.\n\nRegards,\nEmployee Help Desk Team",
                agentName != null ? agentName : "Agent",
                ticketNumber,
                reqFirstName != null ? reqFirstName : "",
                reqLastName != null ? reqLastName : "",
                deptName, catName, subCatName, priority, description);
        sendEmailAsync(toEmail, subject, body);
    }

    @Override
    public void sendTicketStatusChangedEmail(HDTicket ticket, TicketStatus oldStatus, TicketStatus newStatus) {
        if (ticket == null || ticket.getRequester() == null || ticket.getRequester().getEmail() == null) {
            return;
        }
        String toEmail = ticket.getRequester().getEmail();
        String requesterName = ticket.getRequester().getFirstName();
        String ticketNumber = ticket.getTicketNumber();
        String oldStatusStr = oldStatus != null ? oldStatus.name() : "N/A";
        String newStatusStr = newStatus != null ? newStatus.name() : "N/A";

        String subject = String.format("Ticket %s Status Updated: %s", ticketNumber, newStatusStr);
        String body = String.format(
                "Hello %s,\n\nThe status of your ticket %s has been updated from %s to %s.\n\nRegards,\nEmployee Help Desk Team",
                requesterName != null ? requesterName : "User",
                ticketNumber, oldStatusStr, newStatusStr);
        sendEmailAsync(toEmail, subject, body);
    }

    @Override
    public void sendSlaWarningEmail(HDTicket ticket) {
        if (ticket == null || ticket.getAssignedAgent() == null || ticket.getAssignedAgent().getEmail() == null) {
            return;
        }
        String toEmail = ticket.getAssignedAgent().getEmail();
        String agentName = ticket.getAssignedAgent().getFirstName();
        String ticketNumber = ticket.getTicketNumber();
        String priority = ticket.getPriority() != null ? ticket.getPriority().name() : "MEDIUM";
        String status = ticket.getStatus() != null ? ticket.getStatus().name() : "IN_PROGRESS";

        String subject = String.format("URGENT: SLA Warning for Ticket %s", ticketNumber);
        String body = String.format(
                "Hello %s,\n\nTicket %s has reached its SLA warning threshold (75%% SLA consumed).\n\nPriority: %s\nStatus: %s\n\nPlease resolve it before SLA breach occurs.\n\nRegards,\nEmployee Help Desk Team",
                agentName != null ? agentName : "Agent",
                ticketNumber, priority, status);
        sendEmailAsync(toEmail, subject, body);
    }

    @Override
    public void sendSlaBreachedEmail(HDTicket ticket) {
        if (ticket == null) {
            return;
        }
        String ticketNumber = ticket.getTicketNumber();
        String requesterEmail = ticket.getRequester() != null ? ticket.getRequester().getEmail() : "N/A";
        String agentEmail = ticket.getAssignedAgent() != null ? ticket.getAssignedAgent().getEmail() : "Unassigned";
        String deptName = ticket.getDepartment() != null ? ticket.getDepartment().getName() : "N/A";
        String priority = ticket.getPriority() != null ? ticket.getPriority().name() : "MEDIUM";
        String status = ticket.getStatus() != null ? ticket.getStatus().name() : "IN_PROGRESS";

        String subject = String.format("CRITICAL: SLA Breached for Ticket %s", ticketNumber);
        String body = String.format(
                "Hello,\n\nThe SLA resolution deadline for Ticket %s has been BREACHED.\n\nRequester: %s\nAssigned Agent: %s\nDepartment: %s\nPriority: %s\nStatus: %s\n\nImmediate action is required.\n\nRegards,\nEmployee Help Desk Team",
                ticketNumber, requesterEmail, agentEmail, deptName, priority, status);

        if (ticket.getAssignedAgent() != null && ticket.getAssignedAgent().getEmail() != null) {
            sendEmailAsync(ticket.getAssignedAgent().getEmail(), subject, body);
        }
    }

    @Override
    public void sendTicketEscalatedEmail(HDTicket ticket, HDEmployee manager) {
        if (ticket == null || manager == null || manager.getEmail() == null) {
            return;
        }
        String toEmail = manager.getEmail();
        String managerName = manager.getFirstName();
        String ticketNumber = ticket.getTicketNumber();
        String agentEmail = ticket.getAssignedAgent() != null ? ticket.getAssignedAgent().getEmail() : "Unassigned";
        String deptName = ticket.getDepartment() != null ? ticket.getDepartment().getName() : "N/A";
        String priority = ticket.getPriority() != null ? ticket.getPriority().name() : "MEDIUM";
        String status = ticket.getStatus() != null ? ticket.getStatus().name() : "IN_PROGRESS";
        String description = ticket.getDescription() != null ? ticket.getDescription() : "";

        String subject = String.format("ESCALATION: Ticket %s Escalated to You", ticketNumber);
        String body = String.format(
                "Hello %s,\n\nTicket %s has been ESCALATED to you due to an SLA breach.\n\nAssigned Agent: %s\nDepartment: %s\nPriority: %s\nStatus: %s\n\nDescription:\n%s\n\nAs the manager, you can now review and resolve this ticket.\n\nRegards,\nEmployee Help Desk Team",
                managerName != null ? managerName : "Manager",
                ticketNumber, agentEmail, deptName, priority, status, description);
        sendEmailAsync(toEmail, subject, body);
    }

    @Override
    public void sendFeedbackSubmittedEmail(HDTicket ticket, HDTicketFeedback feedback) {
        if (ticket == null || ticket.getAssignedAgent() == null || ticket.getAssignedAgent().getEmail() == null) {
            return;
        }
        String toEmail = ticket.getAssignedAgent().getEmail();
        String agentName = ticket.getAssignedAgent().getFirstName();
        String ticketNumber = ticket.getTicketNumber();
        int rating = feedback != null && feedback.getRating() != null ? feedback.getRating() : 5;

        String subject = String.format("Feedback Received for Ticket %s", ticketNumber);
        String body = String.format(
                "Hello %s,\n\nThe requester submitted feedback for ticket %s:\n\nRating: %d / 5\n\nRegards,\nEmployee Help Desk Team",
                agentName != null ? agentName : "Agent",
                ticketNumber, rating);
        sendEmailAsync(toEmail, subject, body);
    }

    @Override
    public void sendNewMessageNotificationEmail(HDTicket ticket, HDTicketMessage message, HDEmployee recipient) {
        if (ticket == null || recipient == null || recipient.getEmail() == null) {
            return;
        }
        String toEmail = recipient.getEmail();
        String recipientName = recipient.getFirstName();
        String ticketNumber = ticket.getTicketNumber();
        String senderName = (message != null && message.getSender() != null) ? message.getSender().getFirstName() : "User";
        String msgText = message != null && message.getMessageText() != null ? message.getMessageText() : "";

        String subject = String.format("New Message on Ticket %s", ticketNumber);
        String body = String.format(
                "Hello %s,\n\nA new message was posted on ticket %s by %s:\n\n\"%s\"\n\nRegards,\nEmployee Help Desk Team",
                recipientName != null ? recipientName : "User",
                ticketNumber, senderName, msgText);
        sendEmailAsync(toEmail, subject, body);
    }

    @Async
    @Override
    public void sendEmailAsync(String to, String subject, String body) {
        if (to == null || to.isBlank()) {
            log.warn("Cannot send email: recipient address is null or empty");
            return;
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("JavaMailSender is not configured. Skipping email to [{}]: {}", to, subject);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            String sender = (fromEmail != null && !fromEmail.isBlank()) ? fromEmail : "noreply@helpdesk.com";
            message.setFrom(sender);
            message.setTo(to.trim());
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email successfully sent to: {} with subject: {}", to, subject);
        } catch (Exception e) {
            // Log recipient and non-sensitive error without passwords or secrets
            log.error("Failed to send email to recipient [{}]: {} - Cause: {}", to, e.getMessage(),
                    e.getCause() != null ? e.getCause().getMessage() : "N/A");
        }
    }
}
