package com.example.helpdesk.service;

<<<<<<< HEAD
import com.example.helpdesk.dto.request.AssignTicketRequest;
import com.example.helpdesk.dto.request.CreateTicketRequest;
import com.example.helpdesk.dto.request.ReopenTicketRequest;
import com.example.helpdesk.dto.request.UpdateTicketCategoryRequest;
import com.example.helpdesk.dto.request.UpdateTicketPriorityRequest;
import com.example.helpdesk.dto.request.UpdateTicketStatusRequest;
import com.example.helpdesk.dto.response.TicketResponse;
=======
import com.example.helpdesk.dto.request.*;
import com.example.helpdesk.dto.response.AssignmentProposalResponseDTO;
import com.example.helpdesk.dto.response.TicketAttachmentResponseDTO;
import com.example.helpdesk.dto.response.TicketFeedbackResponseDTO;
import com.example.helpdesk.dto.response.TicketMessageResponseDTO;
import com.example.helpdesk.dto.response.TicketResponseDTO;
import com.example.helpdesk.entity.TicketAttachment;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
>>>>>>> c5b2367e (updated reworks)

import java.util.List;

public interface TicketService {

<<<<<<< HEAD
    TicketResponse createTicket(CreateTicketRequest request);

    TicketResponse updateStatus(Long ticketId, UpdateTicketStatusRequest request);
=======
    TicketResponseDTO createTicketWithAttachment(CreateTicketRequestDTO request, Long uploadedById, MultipartFile file);

    TicketResponseDTO createTicket(CreateTicketRequestDTO request);

    TicketResponseDTO updateStatus(Long ticketId, UpdateTicketStatusRequestDTO request);

    TicketResponseDTO updatePriority(Long ticketId, UpdateTicketPriorityRequestDTO request);
>>>>>>> c5b2367e (updated reworks)

    TicketResponseDTO updateCategory(Long ticketId, UpdateTicketCategoryRequestDTO request);

    TicketResponseDTO updateTicket(Long ticketId, UpdateTicketRequestDTO request);

<<<<<<< HEAD
    TicketResponse assignTicket(Long ticketId, AssignTicketRequest request);
=======
    TicketResponseDTO assignTicket(Long ticketId, AssignTicketRequestDTO request);

    TicketResponseDTO resolveTicket(Long ticketId, UpdateTicketStatusRequestDTO request);
>>>>>>> c5b2367e (updated reworks)

    TicketResponseDTO reopenTicket(Long ticketId, ReopenTicketRequestDTO request);

    TicketResponseDTO holdTicket(Long ticketId, HoldTicketRequestDTO request);

<<<<<<< HEAD
}
=======
    TicketResponseDTO resumeTicket(Long ticketId);

    TicketResponseDTO resolveTicketWithSummary(Long ticketId, ResolveTicketRequestDTO request);

    TicketResponseDTO reopenTicketWithSla(Long ticketId);

    TicketResponseDTO withdrawTicket(Long ticketId, String withdrawalReason);

    AssignmentProposalResponseDTO getAssignmentProposal(Long ticketId);

    void confirmAssignment(Long ticketId, Long agentId, Boolean confirmed);

    TicketMessageResponseDTO sendMessage(Long ticketId, TicketMessageRequestDTO request);

    TicketMessageResponseDTO sendMessageWithAttachment(Long ticketId, TicketMessageRequestDTO request, Long uploadedById, MultipartFile file);

    List<TicketMessageResponseDTO> getTicketMessages(Long ticketId);

    void markMessageAsSeen(Long messageId);

    List<TicketMessageResponseDTO> getUnreadMessages(Long ticketId, Long recipientId);

    TicketAttachmentResponseDTO uploadAttachment(Long ticketId, Long uploadedById, MultipartFile file, Long messageId, String attachmentType);

    List<TicketAttachmentResponseDTO> getTicketAttachments(Long ticketId);

    Resource downloadAttachment(Long attachmentId);

    TicketAttachment getAttachmentById(Long attachmentId);

    void deleteAttachment(Long attachmentId, Long requesterId);

    TicketAttachmentResponseDTO uploadMessageAttachment(Long ticketId, Long messageId, Long uploadedById, MultipartFile file);

    TicketFeedbackResponseDTO submitFeedback(Long ticketId, TicketFeedbackRequestDTO request);

    Optional<TicketFeedbackResponseDTO> getTicketFeedback(Long ticketId);
}
>>>>>>> c5b2367e (updated reworks)
