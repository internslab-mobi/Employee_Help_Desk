package xyz.mobi.employeehelpdesk.dto.message;

import java.util.List;

public record TicketMessageResponseDTO(
        Long id,
        Long ticketId,
        Long senderId,
        String senderName,
        String content,
        Boolean seen,
        List<TicketAttachmentResponseDTO> attachments
) {
}
