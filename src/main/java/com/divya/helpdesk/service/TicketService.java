package com.divya.helpdesk.service;

import com.divya.helpdesk.dto.ticket.*;
import com.divya.helpdesk.enums.TicketStatus;
import com.divya.helpdesk.dto.PageResponse;

import java.util.List;

public interface TicketService {

    // Raise ticket
    CreateTicketResponseDTO createTicket(CreateTicketRequestDTO request);

    // Employee / Manager / Admin - edit ticket
    CreateTicketResponseDTO updateTicket(Long ticketId, UpdateTicketRequestDTO request);

    // Employee - own tickets
    List<CreateTicketResponseDTO> getMyTickets(TicketStatus status);

    // Employee - withdraw own ticket
    void withdrawTicket(Long ticketId, WithdrawTicketRequestDTO request);

    // Agent / Manager - assigned tickets
    List<CreateTicketResponseDTO> getAssignedTickets(TicketStatus status);

    // Manager - department tickets
    PageResponse<CreateTicketResponseDTO> getDepartmentTickets(Long agentId, TicketStatus status, int limit, long offset);

    // Single flexible PATCH endpoint
    CreateTicketResponseDTO patchTicket(Long ticketId, TicketPatchRequestDTO request);

    // Agent - start working on assigned ticket (SLA starts from workStartedAt)
    CreateTicketResponseDTO startWorking(Long ticketId);

    // Agent / Manager / Admin - resolve ticket
    CreateTicketResponseDTO resolveTicket(Long ticketId, ResolveTicketRequestDTO request);

    // Agent - waiting for employee (SLA does not pause)
    CreateTicketResponseDTO waitingForEmployee(Long ticketId, WaitingForEmployeeRequestDTO request);

    // Agent - resume after employee response
    CreateTicketResponseDTO resumeTicket(Long ticketId);

    // Employee - reopen resolved ticket (50% SLA allocation)
    CreateTicketResponseDTO reopenTicket(Long ticketId, ReopenTicketRequestDTO request);

    // Employee - send feedback
    void submitFeedback(Long ticketId, FeedbackRequestDTO request);

    // Employee / Agent / Manager - send message
    TicketMessageResponseDTO sendMessage(Long ticketId, SendMessageRequestDTO request);

    // Manager / Admin - delete ticket
    void deleteTicket(Long ticketId);
}