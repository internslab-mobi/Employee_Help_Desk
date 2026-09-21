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
    TicketResponse createTicket(TicketCreateRequest request, Long employeeId);
    List<TicketResponse> getTickets(Long requesterId, String role,  Long departmentId, Long agentId, HDTicketStatus status, HDPriorityLevel priority);
    TicketResponse getTicketById(Long id, Long employeeId);
    TicketResponse getTicketByTicketNumber(String ticketNumber, Long employeeId);
    TicketDetailsResponse getTicketDetails(Long id, Long employeeId);
    TicketResponse updateTicket(Long ticketId, TicketUpdateRequest request, Long employeeId);
    TicketResponse startWorkingOnTicket(Long id, Long employeeId);
    TicketResponse resolveTicket(Long id, TicketResolveRequest request, Long employeeId);
    void deleteTicket(Long id, Long employeeId);
}
