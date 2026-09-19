package com.example.helpdesk.service;

import com.example.helpdesk.dto.request.AssignTicketRequest;
import com.example.helpdesk.dto.request.CreateTicketRequest;
import com.example.helpdesk.dto.request.ReopenTicketRequest;
import com.example.helpdesk.dto.request.UpdateTicketCategoryRequest;
import com.example.helpdesk.dto.request.UpdateTicketPriorityRequest;
import com.example.helpdesk.dto.request.UpdateTicketStatusRequest;
import com.example.helpdesk.dto.response.TicketResponse;

import java.util.List;

public interface TicketService {

    TicketResponse createTicket(CreateTicketRequest request);

    TicketResponse updateStatus(Long ticketId, UpdateTicketStatusRequest request);

    TicketResponse updatePriority(Long ticketId, UpdateTicketPriorityRequest request);

    TicketResponse updateCategory(Long ticketId, UpdateTicketCategoryRequest request);

    TicketResponse assignTicket(Long ticketId, AssignTicketRequest request);

    TicketResponse resolveTicket(Long ticketId, UpdateTicketStatusRequest request);

    TicketResponse reopenTicket(Long ticketId, ReopenTicketRequest request);

}