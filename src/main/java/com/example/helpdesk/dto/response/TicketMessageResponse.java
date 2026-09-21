package com.example.helpdesk.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketMessageResponse {

    private Long id;
    private Long ticketId;
    private Long senderId;
    private String senderName;
    private String content;
    private Boolean seen;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
