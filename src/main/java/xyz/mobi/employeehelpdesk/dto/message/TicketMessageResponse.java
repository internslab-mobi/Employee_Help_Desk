package xyz.mobi.employeehelpdesk.dto.message;

import java.time.LocalDateTime;
import java.util.List;

public record TicketMessageResponse(
        Long id,
        Long ticketId,
        Long senderId,
        String senderName,
        String content,
        Boolean seen,
        LocalDateTime createdAt,
        List<TicketAttachmentResponse> attachments
) {
}
