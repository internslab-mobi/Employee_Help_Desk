package com.example.helpdesk.service.impl;

import com.example.helpdesk.dto.response.TicketAttachmentResponse;
import com.example.helpdesk.entity.Employee;
import com.example.helpdesk.entity.Ticket;
import com.example.helpdesk.entity.TicketAttachment;
import com.example.helpdesk.entity.TicketMessage;
import com.example.helpdesk.enums.NotificationType;
import com.example.helpdesk.enums.TicketEventType;
import com.example.helpdesk.repository.EmployeeRepository;
import com.example.helpdesk.repository.TicketAttachmentRepository;
import com.example.helpdesk.repository.TicketMessageRepository;
import com.example.helpdesk.repository.TicketRepository;
import com.example.helpdesk.service.NotificationService;
import com.example.helpdesk.service.TicketAttachmentService;
import com.example.helpdesk.service.TicketHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TicketAttachmentServiceImpl implements TicketAttachmentService {

    private final TicketAttachmentRepository ticketAttachmentRepository;
    private final TicketRepository ticketRepository;
    private final EmployeeRepository employeeRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final TicketHistoryService ticketHistoryService;
    private final NotificationService notificationService;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final List<String> ALLOWED_MIME_TYPES = List.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    @Override
    public TicketAttachmentResponse uploadAttachment(Long ticketId, Long uploadedById, MultipartFile file, Long messageId, String attachmentType) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        Employee uploader = employeeRepository.findById(uploadedById)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + uploadedById));

        validateUploader(ticket, uploader);
        validateFile(file);

        TicketMessage message = null;
        if (messageId != null) {
            message = ticketMessageRepository.findById(messageId)
                    .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));
            if (!message.getTicket().getId().equals(ticketId)) {
                throw new IllegalArgumentException("Message does not belong to this ticket");
            }
        }

        try {
            byte[] fileData = file.getBytes();
            TicketAttachment attachment = TicketAttachment.builder()
                    .ticket(ticket)
                    .uploadedBy(uploader)
                    .message(message)
                    .originalFilename(file.getOriginalFilename())
                    .mimeType(file.getContentType())
                    .fileSize((long) fileData.length)
                    .fileData(fileData)
                    .attachmentType(attachmentType != null ? attachmentType : "GENERAL")
                    .build();

            attachment = ticketAttachmentRepository.save(attachment);

            ticketHistoryService.recordHistory(
                    ticket,
                    uploader,
                    TicketEventType.ATTACHMENT_ADDED,
                    null,
                    "Attachment added: " + file.getOriginalFilename(),
                    null
            );

            if (message != null) {
                Employee recipient = ticket.getRequester().getId().equals(uploader.getId())
                        ? ticket.getAssignedAgent().getEmployee()
                        : ticket.getRequester();
                notificationService.sendNotification(
                        recipient,
                        ticket,
                        NotificationType.ATTACHMENT_ADDED,
                        "New Attachment",
                        "A new attachment was added to a message on ticket " + ticket.getTicketNumber()
                );
            }

            log.info("Attachment uploaded for ticket {} by {}", ticketId, uploadedById);

            return toResponse(attachment);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file data", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketAttachmentResponse> getTicketAttachments(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        return ticketAttachmentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadAttachment(Long attachmentId) {
        TicketAttachment attachment = ticketAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("Attachment not found: " + attachmentId));

        return new ByteArrayResource(
                attachment.getFileData(),
                attachment.getOriginalFilename()
        );
    }

    @Override
    public void deleteAttachment(Long attachmentId, Long requesterId) {
        TicketAttachment attachment = ticketAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("Attachment not found: " + attachmentId));

        Employee requester = employeeRepository.findById(requesterId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + requesterId));

        if (!attachment.getUploadedBy().getId().equals(requesterId)) {
            throw new IllegalStateException("Only the uploader can delete an attachment");
        }

        ticketAttachmentRepository.delete(attachment);
        log.info("Attachment {} deleted by {}", attachmentId, requesterId);
    }

    @Override
    public TicketAttachmentResponse uploadMessageAttachment(Long ticketId, Long messageId, Long uploadedById, MultipartFile file) {
        return uploadAttachment(ticketId, uploadedById, file, messageId, "MESSAGE");
    }

    private void validateUploader(Ticket ticket, Employee uploader) {
        boolean isRequester = ticket.getRequester().getId().equals(uploader.getId());
        boolean isAssignedAgent = ticket.getAssignedAgent() != null
                && ticket.getAssignedAgent().getEmployee().getId().equals(uploader.getId());

        if (!isRequester && !isAssignedAgent) {
            throw new IllegalStateException("Uploader must be the requester or assigned agent");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be blank");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum limit of 10MB");
        }

        String mimeType = file.getContentType();
        if (mimeType == null || !ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new IllegalArgumentException("File type not allowed: " + mimeType);
        }
    }

    private TicketAttachmentResponse toResponse(TicketAttachment attachment) {
        return TicketAttachmentResponse.builder()
                .id(attachment.getId())
                .ticketId(attachment.getTicket().getId())
                .uploadedById(attachment.getUploadedBy().getId())
                .uploadedByName(attachment.getUploadedBy().getFirstName() + " " + attachment.getUploadedBy().getLastName())
                .messageId(attachment.getMessage() != null ? attachment.getMessage().getId() : null)
                .originalFilename(attachment.getOriginalFilename())
                .mimeType(attachment.getMimeType())
                .fileSize(attachment.getFileSize())
                .attachmentType(attachment.getAttachmentType())
                .createdAt(attachment.getCreatedAt())
                .updatedAt(attachment.getUpdatedAt())
                .build();
    }
}
