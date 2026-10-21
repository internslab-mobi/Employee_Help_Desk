package xyz.mobi.employeehelpdesk.dto.message;

import com.fasterxml.jackson.annotation.JsonInclude;
import xyz.mobi.employeehelpdesk.entity.enums.AttachmentType;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TicketAttachmentResponseDTO(
        Long id,
        String originalFilename,
        String mimeType,
        Long fileSize,
        AttachmentType attachmentType
) {
}
