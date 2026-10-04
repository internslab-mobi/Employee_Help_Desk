package xyz.mobi.employeehelpdesk.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import xyz.mobi.employeehelpdesk.dto.feedback.TicketFeedbackCreateResponseDTO;
import xyz.mobi.employeehelpdesk.dto.feedback.TicketFeedbackRequestDTO;
import xyz.mobi.employeehelpdesk.dto.feedback.TicketFeedbackResponseDTO;
import xyz.mobi.employeehelpdesk.dto.ticket.*;
import xyz.mobi.employeehelpdesk.entity.enums.TicketStatus;
import xyz.mobi.employeehelpdesk.entity.enums.TicketView;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface TicketService {

    TicketCreateResponseDTO createTicket(
            CreateTicketRequestDTO request,
            List<MultipartFile> attachments)
            throws IOException;

    TicketResponseDTO getTicket(Long ticketId);

    Page<TicketResponseDTO> getAllTickets(
            TicketView view,
            Long employeeId,
            Pageable pageable
    );

    Page<TicketResponseDTO> searchTickets(
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

    List<AssignableAgentResponseDTO> getAssignableAgents(Long ticketId);

    Map<String, Integer> getTicketSummary(
            TicketView view,
            Long employeeId
    );

    TicketUpdateResponseDTO updateTicket(
            Long ticketId,
            UpdateTicketRequestDTO request
    );

    TicketUpdateResponseDTO assignTicketByManager(Long ticketId, Long agentId);

    TicketUpdateResponseDTO withdrawTicket(Long ticketId, WithdrawRequestDTO withdrawRequest);

    TicketUpdateResponseDTO startTicket(Long ticketId);

    TicketUpdateResponseDTO holdTicket(Long ticketId, HoldTicketRequestDTO request);

    TicketUpdateResponseDTO resumeTicket(Long ticketId);

    TicketUpdateResponseDTO resolveTicket(Long ticketId, ResolveTicketRequestDTO request);

    TicketUpdateResponseDTO reopenTicket(Long ticketId, ReopenRequestDTO request);

    // feedback

    TicketFeedbackCreateResponseDTO createFeedback(Long ticketId, TicketFeedbackRequestDTO request);

    TicketFeedbackResponseDTO getFeedback(Long ticketId);
}
