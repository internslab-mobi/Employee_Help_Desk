package com.example.helpdesk.controller;

import com.example.helpdesk.dto.response.TicketAttachmentResponse;
import com.example.helpdesk.service.TicketAttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketAttachmentController {

    private final TicketAttachmentService ticketAttachmentService;

    @PostMapping("/{ticketId}/attachments")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    public ResponseEntity<TicketAttachmentResponse> uploadAttachment(
            @PathVariable Long ticketId,
            @RequestParam Long uploadedById,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String attachmentType) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ticketAttachmentService.uploadAttachment(ticketId, uploadedById, file, null, attachmentType));
    }

    @GetMapping("/{ticketId}/attachments")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<TicketAttachmentResponse>> getTicketAttachments(
            @PathVariable Long ticketId) {

        return ResponseEntity.ok(ticketAttachmentService.getTicketAttachments(ticketId));
    }

    @GetMapping("/ticket-attachments/{attachmentId}/download")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable Long attachmentId) {

        Resource resource = ticketAttachmentService.downloadAttachment(attachmentId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @DeleteMapping("/ticket-attachments/{attachmentId}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    public ResponseEntity<Void> deleteAttachment(
            @PathVariable Long attachmentId,
            @RequestParam Long requesterId) {

        ticketAttachmentService.deleteAttachment(attachmentId, requesterId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{ticketId}/messages/{messageId}/attachments")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    public ResponseEntity<TicketAttachmentResponse> uploadMessageAttachment(
            @PathVariable Long ticketId,
            @PathVariable Long messageId,
            @RequestParam Long uploadedById,
            @RequestParam("file") MultipartFile file) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ticketAttachmentService.uploadMessageAttachment(ticketId, messageId, uploadedById, file));
    }
}
