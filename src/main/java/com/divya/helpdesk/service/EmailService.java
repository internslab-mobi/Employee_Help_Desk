package com.divya.helpdesk.service;

import com.divya.helpdesk.entity.*;
import com.divya.helpdesk.enums.TicketStatus;

public interface EmailService {

    void sendOtpEmail(String toEmail, String otp);

    void sendTemporaryPasswordEmail(String toEmail, String employeeName, String employeeCode, String temporaryPassword);

    void sendTicketCreatedEmail(HDTicketEntity ticket);

    void sendTicketAssignedEmail(HDTicketEntity ticket, HDEmployeeEntity agent);

    void sendTicketStatusChangedEmail(HDTicketEntity ticket, TicketStatus oldStatus, TicketStatus newStatus);

    void sendSlaBreachedEmail(HDTicketEntity ticket);

    void sendSlaWarningEmail(HDTicketEntity ticket);

    void sendTicketEscalatedEmail(HDTicketEntity ticket, HDEmployeeEntity manager);

    void sendFeedbackSubmittedEmail(HDTicketEntity ticket, HDTicketFeedbackEntity feedback);

    void sendNewMessageNotificationEmail(HDTicketEntity ticket, HDTicketMessageEntity message, HDEmployeeEntity recipient);

    // Primitive/String-based methods to prevent Hibernate proxy leaks across async threads
    void sendEmailAsync(String to, String subject, String body);
}

