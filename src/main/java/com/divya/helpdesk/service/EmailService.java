package com.divya.helpdesk.service;

import com.divya.helpdesk.entity.HDEmployee;
import com.divya.helpdesk.entity.HDTicket;
import com.divya.helpdesk.entity.HDTicketFeedback;
import com.divya.helpdesk.entity.HDTicketMessage;
import com.divya.helpdesk.enums.TicketStatus;

public interface EmailService {

    void sendOtpEmail(String toEmail, String otp);

    void sendTemporaryPasswordEmail(String toEmail, String employeeName, String employeeCode, String temporaryPassword);

    void sendAccountActivatedEmail(String toEmail, String employeeName);

    void sendTicketCreatedEmail(HDTicket ticket);

    void sendTicketAssignedEmail(HDTicket ticket, HDEmployee agent);

    void sendTicketStatusChangedEmail(HDTicket ticket, TicketStatus oldStatus, TicketStatus newStatus);

    void sendSlaBreachedEmail(HDTicket ticket);

    void sendSlaWarningEmail(HDTicket ticket);

    void sendTicketEscalatedEmail(HDTicket ticket, HDEmployee manager);

    void sendFeedbackSubmittedEmail(HDTicket ticket, HDTicketFeedback feedback);

    void sendNewMessageNotificationEmail(HDTicket ticket, HDTicketMessage message, HDEmployee recipient);

    // Primitive/String-based methods to prevent Hibernate proxy leaks across async threads
    void sendEmailAsync(String to, String subject, String body);
}

