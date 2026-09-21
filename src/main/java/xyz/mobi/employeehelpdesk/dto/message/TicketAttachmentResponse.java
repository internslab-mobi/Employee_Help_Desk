package xyz.mobi.employeehelpdesk.dto.message;

import xyz.mobi.employeehelpdesk.entity.enums.AttachmentType;

import java.time.LocalDateTime;

public record TicketAttachmentResponse(
        Long id,
        String originalFilename,
        String mimeType,
        Long fileSize,
        AttachmentType attachmentType,
        LocalDateTime createdAt
) {
}
