package xyz.mobi.employeehelpdesk.dto.feedback;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.Instant;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TicketFeedbackResponseDTO(
        Long id,
        Long ticketId,
        Long submittedById,
        String submittedByName,
        Integer rating,
        String comment,
        Instant createdAt,
        Instant updatedAt
) {
}
