package com.divya.helpdesk.dto.ticket;

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
public class TicketMessageResponseDTO {
    private Long id;
    private Long ticketId;
    private MessageSenderDTO sender;
    private String messageText;
    private OffsetDateTime createdAt;
}
