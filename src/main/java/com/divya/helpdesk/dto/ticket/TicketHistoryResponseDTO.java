package com.divya.helpdesk.dto.ticket;

import com.divya.helpdesk.enums.TicketEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketHistoryResponseDTO {
    private Long id;
    private Long ticketId;
    private AgentDTO actor;
    private TicketEventType eventType;
    private String oldValue;
    private String newValue;
    private OffsetDateTime createdAt;
}
