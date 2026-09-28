package com.example.helpdesk.service;

import com.example.helpdesk.dto.request.AssignTicketRequest;
import com.example.helpdesk.dto.request.CreateTicketRequest;
import com.example.helpdesk.dto.request.HoldTicketRequest;
import com.example.helpdesk.dto.request.ReopenTicketRequest;
import com.example.helpdesk.dto.request.ResolveTicketRequest;
import com.example.helpdesk.dto.request.TicketFeedbackRequest;
import com.example.helpdesk.dto.request.TicketMessageRequest;
import com.example.helpdesk.dto.request.UpdateDTO;
import com.example.helpdesk.dto.request.UpdateTicketCategoryRequest;
import com.example.helpdesk.dto.request.UpdateTicketPriorityRequest;
import com.example.helpdesk.dto.request.UpdateTicketRequest;
import com.example.helpdesk.dto.request.UpdateTicketStatusRequest;
import com.example.helpdesk.dto.response.AssignmentProposalResponse;
import com.example.helpdesk.dto.response.TicketAttachmentResponse;
import com.example.helpdesk.dto.response.TicketFeedbackResponse;
import com.example.helpdesk.dto.response.TicketMessageResponse;
import com.example.helpdesk.dto.response.TicketResponse;
import com.example.helpdesk.entity.TicketAttachment;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

public interface TicketService {

    // ==================== TICKET LIFECYCLE ====================

    TicketResponse createTicket(CreateTicketRequest request);

    TicketResponse createTicketWithAttachment(CreateTicketRequest request, Long uploadedById, MultipartFile file);

    TicketResponse updateStatus(Long ticketId, UpdateTicketStatusRequest request);

    TicketResponse updatePriority(Long ticketId, UpdateTicketPriorityRequest request);

    TicketResponse updateCategory(Long ticketId, UpdateTicketCategoryRequest request);

    TicketResponse updateTicket(Long ticketId, UpdateDTO request);

    TicketResponse assignTicket(Long ticketId, AssignTicketRequest request);

    TicketResponse resolveTicket(Long ticketId, UpdateTicketStatusRequest request);

    TicketResponse reopenTicket(Long ticketId, ReopenTicketRequest request);

    TicketResponse holdTicket(Long ticketId, HoldTicketRequest request);

    TicketResponse resumeTicket(Long ticketId);

    TicketResponse resolveTicketWithSummary(Long ticketId, ResolveTicketRequest request);

    TicketResponse reopenTicketWithSla(Long ticketId);

    TicketResponse withdrawTicket(Long ticketId, String withdrawalReason);

    // ==================== ROUTING ====================

    AssignmentProposalResponse getAssignmentProposal(Long ticketId);

    void confirmAssignment(Long ticketId, Long agentId, Boolean confirmed);

    // ==================== MESSAGES ====================

    TicketMessageResponse sendMessage(Long ticketId, TicketMessageRequest request);

    TicketMessageResponse sendMessageWithAttachment(Long ticketId, TicketMessageRequest request, Long uploadedById, MultipartFile file);

    List<TicketMessageResponse> getTicketMessages(Long ticketId);

    void markMessageAsSeen(Long messageId);

    List<TicketMessageResponse> getUnreadMessages(Long ticketId, Long recipientId);

    // ==================== ATTACHMENTS ====================

    TicketAttachmentResponse uploadAttachment(Long ticketId, Long uploadedById, MultipartFile file, Long messageId, String attachmentType);

    List<TicketAttachmentResponse> getTicketAttachments(Long ticketId);

    Resource downloadAttachment(Long attachmentId);

    TicketAttachment getAttachmentById(Long attachmentId);

    void deleteAttachment(Long attachmentId, Long requesterId);

    TicketAttachmentResponse uploadMessageAttachment(Long ticketId, Long messageId, Long uploadedById, MultipartFile file);

    // ==================== FEEDBACK ====================

    TicketFeedbackResponse submitFeedback(Long ticketId, TicketFeedbackRequest request);

    Optional<TicketFeedbackResponse> getTicketFeedback(Long ticketId);

}