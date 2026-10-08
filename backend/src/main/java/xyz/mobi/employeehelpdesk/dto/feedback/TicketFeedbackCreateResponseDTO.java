package xyz.mobi.employeehelpdesk.dto.feedback;

import java.time.Instant;

public record TicketFeedbackCreateResponseDTO(
        Long id,
        Long ticketId,
        Long submittedById,
        String submittedByName,
        Integer rating,
        String comment,
        Instant createdAt
) {
}
