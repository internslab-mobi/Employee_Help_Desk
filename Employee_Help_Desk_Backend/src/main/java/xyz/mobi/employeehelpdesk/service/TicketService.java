package xyz.mobi.employeehelpdesk.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import xyz.mobi.employeehelpdesk.dto.feedback.TicketFeedbackRequestDTO;
import xyz.mobi.employeehelpdesk.dto.feedback.TicketFeedbackResponseDTO;
import xyz.mobi.employeehelpdesk.dto.ticket.*;
import xyz.mobi.employeehelpdesk.entity.enums.TicketStatus;
import xyz.mobi.employeehelpdesk.entity.enums.TicketView;
import xyz.mobi.employeehelpdesk.entity.enums.Priority;
import xyz.mobi.employeehelpdesk.entity.enums.SlaStatus;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface TicketService {

    TicketResponseDTO createTicket(
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
            Long departmentId,
            Long agentId,
            TicketStatus status,
            Priority priority,
            LocalDate fromDate,
            LocalDate toDate,
            String search,
            Boolean unassigned,
            SlaStatus slaStatus,
            Pageable pageable
    );

    List<AssignableAgentResponseDTO> getAssignableAgents(Long ticketId);

    Map<String, Integer> getTicketSummary(
            TicketView view,
            Long employeeId,
            Long departmentId
    );

    TicketResponseDTO updateTicket(
            Long ticketId,
            UpdateTicketRequestDTO request
    );

    TicketResponseDTO assignTicketByManager(Long ticketId, Long agentId);

    TicketResponseDTO withdrawTicket(Long ticketId, WithdrawRequestDTO withdrawRequest);

    TicketResponseDTO startTicket(Long ticketId);

    TicketResponseDTO holdTicket(Long ticketId, HoldTicketRequestDTO request);

    TicketResponseDTO resumeTicket(Long ticketId);

    TicketResponseDTO resolveTicket(Long ticketId, ResolveTicketRequestDTO request);

    TicketResponseDTO reopenTicket(Long ticketId, ReopenRequestDTO request);

    // feedback

    TicketFeedbackResponseDTO createFeedback(Long ticketId, TicketFeedbackRequestDTO request);

    TicketFeedbackResponseDTO getFeedback(Long ticketId);

    List<TicketHistoryResponseDTO> getTicketHistory(Long ticketId);

    TicketSlaResponseDTO getTicketSla(Long ticketId);

    List<xyz.mobi.employeehelpdesk.dto.message.TicketAttachmentResponseDTO> getTicketAttachments(Long ticketId);
}
