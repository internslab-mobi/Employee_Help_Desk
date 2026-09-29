package com.divya.helpdesk.service;

import com.divya.helpdesk.dto.ticket.*;
import com.divya.helpdesk.enums.TicketStatus;
import com.divya.helpdesk.dto.common.PageResponse;

import java.util.List;

public interface TicketService {

    // Raise ticket
    TicketResponse createTicket(CreateTicketRequest request);

    // Employee / Manager / Admin - edit ticket
    TicketResponse updateTicket(Long ticketId, UpdateTicketRequest request);

    // Employee - own tickets
    List<TicketResponse> getMyTickets(TicketStatus status);

    // Employee - withdraw own ticket
    void withdrawTicket(Long ticketId, WithdrawTicketRequest request);

    // Agent / Manager - assigned tickets
    List<TicketResponse> getAssignedTickets(TicketStatus status);

    // Manager - department tickets
    PageResponse<TicketResponse> getDepartmentTickets(Long agentId, TicketStatus status, int limit, long offset);

    // Single flexible PATCH endpoint
    TicketResponse patchTicket(Long ticketId, TicketPatchRequest request);

    // Agent / Manager / Admin - resolve ticket
    TicketResponse resolveTicket(Long ticketId, ResolveTicketRequest request);

    // Agent - waiting for employee (SLA does not pause)
    TicketResponse waitingForEmployee(Long ticketId, WaitingForEmployeeRequest request);

    // Agent - resume after employee response
    TicketResponse resumeTicket(Long ticketId);

    // Employee - reopen resolved ticket (50% SLA allocation)
    TicketResponse reopenTicket(Long ticketId, ReopenTicketRequest request);

    // Employee - send feedback
    void submitFeedback(Long ticketId, FeedbackRequest request);

    // Employee / Agent / Manager - send message
    void sendMessage(Long ticketId, SendMessageRequest request);

    // Manager / Admin - delete ticket
    void deleteTicket(Long ticketId);
}