package xyz.mobi.employeehelpdesk.dto.message;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.Instant;
import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TicketMessageResponseDTO(
        Long id,
        Long ticketId,
        Long senderId,
        String senderName,
        String content,
        Boolean seen,
        Instant createdAt,
        Instant updatedAt,
        List<TicketAttachmentResponseDTO> attachments
) {
}
