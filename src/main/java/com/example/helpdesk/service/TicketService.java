package com.example.helpdesk.service;

import com.example.helpdesk.dto.request.*;
import com.example.helpdesk.dto.response.AssignmentProposalResponseDTO;
import com.example.helpdesk.dto.response.TicketAttachmentResponseDTO;
import com.example.helpdesk.dto.response.TicketFeedbackResponseDTO;
import com.example.helpdesk.dto.response.TicketMessageResponseDTO;
import com.example.helpdesk.dto.response.TicketResponseDTO;
import com.example.helpdesk.entity.TicketAttachment;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

public interface TicketService {

    TicketResponseDTO createTicketWithAttachment(CreateTicketRequestDTO request, Long uploadedById, MultipartFile file);

    TicketResponseDTO createTicket(CreateTicketRequestDTO request);

    TicketResponseDTO updateStatus(Long ticketId, UpdateTicketStatusRequestDTO request);

    TicketResponseDTO updatePriority(Long ticketId, UpdateTicketPriorityRequestDTO request);

    TicketResponseDTO updateCategory(Long ticketId, UpdateTicketCategoryRequestDTO request);

    TicketResponseDTO updateTicket(Long ticketId, UpdateTicketRequestDTO request);

    TicketResponseDTO assignTicket(Long ticketId, AssignTicketRequestDTO request);

    TicketResponseDTO resolveTicket(Long ticketId, UpdateTicketStatusRequestDTO request);

    TicketResponseDTO reopenTicket(Long ticketId, ReopenTicketRequestDTO request);

    TicketResponseDTO holdTicket(Long ticketId, HoldTicketRequestDTO request);

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
