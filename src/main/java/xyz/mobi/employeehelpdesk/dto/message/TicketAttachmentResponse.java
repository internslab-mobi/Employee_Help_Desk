package xyz.mobi.employeehelpdesk.dto.message;

import xyz.mobi.employeehelpdesk.entity.enums.AttachmentType;

public record TicketAttachmentResponse(
        Long id,
        String originalFilename,
        String mimeType,
        Long fileSize,
        AttachmentType attachmentType
) {
}
