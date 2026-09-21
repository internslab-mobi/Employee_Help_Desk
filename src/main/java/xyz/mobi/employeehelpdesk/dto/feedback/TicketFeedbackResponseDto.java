package xyz.mobi.employeehelpdesk.dto.feedback;

import java.time.LocalDateTime;

public record TicketFeedbackResponseDto(
        Long id,
        Long ticketId,
        Long submittedById,
        String submittedByName,
        Integer rating,
        String comment,
        LocalDateTime createdAt
) {
}
