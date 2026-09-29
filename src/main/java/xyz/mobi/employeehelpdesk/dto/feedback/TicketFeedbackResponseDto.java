package xyz.mobi.employeehelpdesk.dto.feedback;

public record TicketFeedbackResponseDto(
        Long id,
        Long ticketId,
        Long submittedById,
        String submittedByName,
        Integer rating,
        String comment
) {
}
