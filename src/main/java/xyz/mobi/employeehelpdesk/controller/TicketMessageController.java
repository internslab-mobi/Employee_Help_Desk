package xyz.mobi.employeehelpdesk.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import xyz.mobi.employeehelpdesk.dto.message.CreateTicketMessageRequest;
import xyz.mobi.employeehelpdesk.dto.message.TicketMessageResponse;
import xyz.mobi.employeehelpdesk.entity.TicketAttachment;
import xyz.mobi.employeehelpdesk.service.TicketMessageService;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketMessageController {

    private final TicketMessageService ticketMessageService;

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    @PostMapping(value = "/{ticketId}/messages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TicketMessageResponse> createMessageMultipart(
            @PathVariable Long ticketId,
            @RequestParam("content") String content,
            @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments
    ) throws IOException {
        TicketMessageResponse response = ticketMessageService.createMessage(ticketId, content, attachments);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    @PostMapping(value = "/{ticketId}/messages", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TicketMessageResponse> createMessageJson(
            @PathVariable Long ticketId,
            @Valid @RequestBody CreateTicketMessageRequest request
    ) throws IOException {
        TicketMessageResponse response = ticketMessageService.createMessage(ticketId, request.content(), null);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    @GetMapping("/{ticketId}/messages")
    public ResponseEntity<Page<TicketMessageResponse>> getMessages(
            @PathVariable Long ticketId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<TicketMessageResponse> response = ticketMessageService.getMessages(ticketId, pageable);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    @GetMapping("/{ticketId}/attachments/{attachmentId}/download")
    public ResponseEntity<byte[]> downloadAttachment(
            @PathVariable Long ticketId,
            @PathVariable Long attachmentId
    ) {
        TicketAttachment attachment = ticketMessageService.getAttachmentForDownload(ticketId, attachmentId);

        MediaType mediaType;
        try {
            mediaType = attachment.getMimeType() != null
                    ? MediaType.parseMediaType(attachment.getMimeType())
                    : MediaType.APPLICATION_OCTET_STREAM;
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.getOriginalFilename() + "\"")
                .body(attachment.getFileData());
    }
}
