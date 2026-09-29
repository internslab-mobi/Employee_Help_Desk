package xyz.mobi.employeehelpdesk.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import xyz.mobi.employeehelpdesk.dto.feedback.TicketFeedbackCreateResponse;
import xyz.mobi.employeehelpdesk.dto.feedback.TicketFeedbackRequestDto;
import xyz.mobi.employeehelpdesk.dto.feedback.TicketFeedbackResponseDto;
import xyz.mobi.employeehelpdesk.dto.ticket.*;
import xyz.mobi.employeehelpdesk.entity.enums.TicketStatus;
import xyz.mobi.employeehelpdesk.entity.enums.TicketView;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface TicketService {

    TicketCreateResponse createTicket(
            CreateTicketRequest request,
            List<MultipartFile> attachments,
            Long requesterId) throws IOException;

    TicketResponse getTicket(Long ticketId);

    Page<TicketResponse> getAllTickets(
            TicketView view,
            Long employeeId,
            Pageable pageable
    );

    Page<TicketResponse> searchTickets(
            TicketView view,
            Long employeeId,
            TicketStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            String search,
            Pageable pageable
    );

    Page<TicketResponse> searchTickets(
            TicketView view,
            Long employeeId,
            TicketStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            String search,
            Boolean unassigned,
            xyz.mobi.employeehelpdesk.entity.enums.SlaStatus slaStatus,
            Pageable pageable
    );

    List<AssignableAgentResponse> getAssignableAgents(Long ticketId);

    Map<String, Integer> getTicketSummary(
            TicketView view,
            Long employeeId
    );

    TicketUpdateResponse updateTicket(
            Long ticketId,
            UpdateTicketRequest request
    );

    TicketUpdateResponse assignTicketByManager(Long ticketId, Long agentId);

    TicketUpdateResponse withdrawTicket(Long ticketId, WithdrawRequestDto withdrawRequest);

    TicketUpdateResponse startTicket(Long ticketId);

    TicketUpdateResponse holdTicket(Long ticketId, HoldTicketRequestDto request);

    TicketUpdateResponse resumeTicket(Long ticketId);

    TicketUpdateResponse resolveTicket(Long ticketId, ResolveTicketRequestDto request);

    TicketUpdateResponse reopenTicket(Long ticketId, ReopenRequestDto request);

    // feedback

    TicketFeedbackCreateResponse createFeedback(Long ticketId, TicketFeedbackRequestDto request);

    TicketFeedbackResponseDto getFeedback(Long ticketId);
}
