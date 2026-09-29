package com.divya.helpdesk.service.impl;

import com.divya.helpdesk.dto.ticket.TicketAttachmentResponse;
import com.divya.helpdesk.entity.HDEmployee;
import com.divya.helpdesk.entity.HDTicket;
import com.divya.helpdesk.entity.HDTicketAttachment;
import com.divya.helpdesk.enums.EmployeeRole;
import com.divya.helpdesk.exception.AccessDeniedException;
import com.divya.helpdesk.exception.ResourceNotFoundException;
import com.divya.helpdesk.exception.ValidationException;
import com.divya.helpdesk.repository.HDTicketAttachmentRepository;
import com.divya.helpdesk.repository.HDTicketRepository;
import com.divya.helpdesk.security.CurrentUserService;
import com.divya.helpdesk.service.AttachmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AttachmentServiceImpl implements AttachmentService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB limit

    private final HDTicketAttachmentRepository attachmentRepository;
    private final HDTicketRepository ticketRepository;
    private final CurrentUserService currentUserService;

    @Override
    public TicketAttachmentResponse uploadAttachment(Long ticketId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("Attachment file cannot be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ValidationException("Attachment file size exceeds the 10MB limit");
        }

        HDTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + ticketId));

        HDEmployee current = currentUserService.getCurrentEmployee();
        validateAccess(ticket, current);

        try {
            HDTicketAttachment attachment = new HDTicketAttachment();
            attachment.setTicket(ticket);
            attachment.setUploadedBy(current);
            attachment.setOriginalFilename(file.getOriginalFilename() != null ? file.getOriginalFilename() : "attachment");
            attachment.setMimeType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");
            attachment.setFileSize(file.getSize());
            attachment.setFileData(file.getBytes());

            HDTicketAttachment saved = attachmentRepository.save(attachment);
            log.info("Uploaded attachment {} ({}) for ticket {}", saved.getId(), saved.getOriginalFilename(), ticket.getTicketNumber());

            return mapToResponse(saved);
        } catch (IOException e) {
            log.error("Failed to read attachment file content: {}", e.getMessage(), e);
            throw new ValidationException("Failed to process uploaded file: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketAttachmentResponse> getAttachments(Long ticketId) {
        HDTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + ticketId));

        HDEmployee current = currentUserService.getCurrentEmployee();
        validateAccess(ticket, current);

        return attachmentRepository.findByTicket_IdOrderByCreatedAtDesc(ticketId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public HDTicketAttachment getAttachment(Long ticketId, Long attachmentId) {
        HDTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + ticketId));

        HDEmployee current = currentUserService.getCurrentEmployee();
        validateAccess(ticket, current);

        return attachmentRepository.findByIdAndTicket_Id(attachmentId, ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found with id: " + attachmentId + " for ticket: " + ticketId));
    }

    @Override
    public void deleteAttachment(Long ticketId, Long attachmentId) {
        HDTicketAttachment attachment = getAttachment(ticketId, attachmentId);
        attachmentRepository.delete(attachment);
        log.info("Deleted attachment {} from ticket {}", attachmentId, ticketId);
    }

    private void validateAccess(HDTicket ticket, HDEmployee current) {
        if (current.getRole() == EmployeeRole.ADMIN) {
            return;
        }

        boolean isRequester = ticket.getRequester() != null && ticket.getRequester().getId().equals(current.getId());
        boolean isAssignedAgent = ticket.getAssignedAgent() != null && ticket.getAssignedAgent().getId().equals(current.getId());
        boolean isAssignedManager = ticket.getAssignedManager() != null && ticket.getAssignedManager().getId().equals(current.getId());
        boolean isDeptManager = current.getRole() == EmployeeRole.MANAGER
                && ticket.getDepartment() != null
                && current.getDepartment() != null
                && ticket.getDepartment().getId().equals(current.getDepartment().getId());

        if (!isRequester && !isAssignedAgent && !isAssignedManager && !isDeptManager) {
            throw new AccessDeniedException("You are not authorized to access attachments for this ticket");
        }
    }

    private TicketAttachmentResponse mapToResponse(HDTicketAttachment attachment) {
        if (attachment == null) {
            return null;
        }

        long size = attachment.getFileSize() != null ? attachment.getFileSize() :
                (attachment.getFileData() != null ? attachment.getFileData().length : 0L);

        return TicketAttachmentResponse.builder()
                .id(attachment.getId())
                .ticketId(attachment.getTicket() != null ? attachment.getTicket().getId() : null)
                .originalFilename(attachment.getOriginalFilename())
                .mimeType(attachment.getMimeType())
                .fileSize(size)
                .createdAt(attachment.getCreatedAt())
                .build();
    }
}
