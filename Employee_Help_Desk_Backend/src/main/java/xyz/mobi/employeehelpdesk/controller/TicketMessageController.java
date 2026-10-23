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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import xyz.mobi.employeehelpdesk.dto.message.CreateTicketMessageRequestDTO;
import xyz.mobi.employeehelpdesk.dto.message.TicketMessageResponseDTO;
import xyz.mobi.employeehelpdesk.entity.TicketAttachment;
import xyz.mobi.employeehelpdesk.security.UserPrincipal;
import xyz.mobi.employeehelpdesk.service.TicketMessageService;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/helpdesk/tickets")
@RequiredArgsConstructor
public class TicketMessageController {

    private final TicketMessageService ticketMessageService;

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    @PostMapping(value = "/{ticketId}/messages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TicketMessageResponseDTO> createMessageMultipart(
            @PathVariable Long ticketId,
            @RequestParam("content") String content,
            @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) throws IOException {
        TicketMessageResponseDTO response = ticketMessageService.createMessage(ticketId, content, attachments, userPrincipal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    @PostMapping(value = "/{ticketId}/messages", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TicketMessageResponseDTO> createMessageJson(
            @PathVariable Long ticketId,
            @Valid @RequestBody CreateTicketMessageRequestDTO request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) throws IOException {
        TicketMessageResponseDTO response = ticketMessageService.createMessage(ticketId, request.content(), null, userPrincipal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    @GetMapping("/{ticketId}/messages")
    public ResponseEntity<Page<TicketMessageResponseDTO>> getMessages(
            @PathVariable Long ticketId,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Page<TicketMessageResponseDTO> response = ticketMessageService.getMessages(ticketId, pageable, userPrincipal);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    @GetMapping("/{ticketId}/attachments/{attachmentId}/download")
    public ResponseEntity<byte[]> downloadAttachment(
            @PathVariable Long ticketId,
            @PathVariable Long attachmentId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        TicketAttachment attachment = ticketMessageService.getAttachmentForDownload(ticketId, attachmentId, userPrincipal);

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
