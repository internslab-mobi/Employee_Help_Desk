package com.divya.helpdesk.dto.ticket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketMessageResponse {
    private Long id;
    private Long ticketId;
    private MessageSenderDTO sender;
    private String messageText;
    private Instant createdAt;
}
