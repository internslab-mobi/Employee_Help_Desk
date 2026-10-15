package xyz.mobi.employeehelpdesk.dto.ticket;

import xyz.mobi.employeehelpdesk.entity.enums.SlaStatus;

import java.time.Instant;

public record TicketSlaResponseDTO(
        Long id,
        String policyName,
        Integer cycleNumber,
        Integer allocatedMinutes,
        Instant slaStartAt,
        Instant originalDeadlineAt,
        Instant currentDeadlineAt,
        Instant warningAt,
        SlaStatus status,
        Instant pausedAt,
        Instant breachedAt
) {
}
