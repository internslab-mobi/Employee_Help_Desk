package xyz.mobi.employeehelpdesk.dto.message;

import java.time.Instant;
import java.util.List;

public record TicketMessageCreateResponseDTO(
        Long id,
        Long ticketId,
        Long senderId,
        String senderName,
        String content,
        Boolean seen,
        Instant createdAt,
        List<TicketAttachmentResponseDTO> attachments
) {
}
