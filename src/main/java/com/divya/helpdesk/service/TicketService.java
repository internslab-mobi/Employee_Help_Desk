package com.divya.helpdesk.service;

import com.divya.helpdesk.dto.request.TicketCreateRequest;
import com.divya.helpdesk.dto.request.TicketResolveRequest;
import com.divya.helpdesk.dto.request.TicketUpdateRequest;
import com.divya.helpdesk.dto.response.TicketDetailsResponse;
import com.divya.helpdesk.dto.response.TicketResponse;
import com.divya.helpdesk.enums.HDPriorityLevel;
import com.divya.helpdesk.enums.HDTicketStatus;

import java.util.List;

public interface TicketService {
    TicketResponse createTicket(TicketCreateRequest request);
    List<TicketResponse> getTickets(Long requesterId, Long departmentId, Long agentId, HDTicketStatus status, HDPriorityLevel priority);
    TicketResponse getTicketById(Long id);
    TicketResponse getTicketByTicketNumber(String ticketNumber);
    TicketDetailsResponse getTicketDetails(Long id);
    TicketResponse updateTicket(Long ticketId, TicketUpdateRequest request);
    TicketResponse startWorkingOnTicket(Long id);
    TicketResponse resolveTicket(Long id, TicketResolveRequest request);
    void deleteTicket(Long id);
}
