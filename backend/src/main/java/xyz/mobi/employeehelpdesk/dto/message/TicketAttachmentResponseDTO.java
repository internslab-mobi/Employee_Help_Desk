package xyz.mobi.employeehelpdesk.dto.message;

import xyz.mobi.employeehelpdesk.entity.enums.AttachmentType;

public record TicketAttachmentResponseDTO(
        Long id,
        String originalFilename,
        String mimeType,
        Long fileSize,
        AttachmentType attachmentType
) {
}
