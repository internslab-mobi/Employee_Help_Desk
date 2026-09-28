package com.example.helpdesk.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

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
    private OffsetDateTime createdAt;
    private List<TicketAttachmentResponse> attachments;
}
