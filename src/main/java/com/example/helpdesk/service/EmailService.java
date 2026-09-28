package com.example.helpdesk.service;

import com.example.helpdesk.entity.Employee;
import com.example.helpdesk.entity.Ticket;

public interface EmailService {

    void sendTicketCreatedEmail(Employee recipient, Ticket ticket);

    void sendTicketHoldEmail(Employee requester, Ticket ticket, String holdReason);

    void sendTicketResolvedEmail(Employee requester, Ticket ticket, String resolutionSummary);

    void sendTicketReopenedEmail(Employee recipient, Ticket ticket);

    void sendTicketWithdrawnEmail(Employee requester, Ticket ticket, String withdrawalReason);

    void sendAccountCreatedEmail(Employee employee, String temporaryPassword, String otp, int otpExpirationMinutes);
}
