package xyz.mobi.employeehelpdesk.dto.ticket;

import xyz.mobi.employeehelpdesk.entity.enums.HistoryEventType;
import xyz.mobi.employeehelpdesk.entity.enums.TicketStatus;

import java.time.Instant;

public record TicketHistoryResponseDTO(
        Long id,
        HistoryEventType eventType,
        TicketStatus oldValue,
        TicketStatus newValue,
        Instant createdAt
) {
}
