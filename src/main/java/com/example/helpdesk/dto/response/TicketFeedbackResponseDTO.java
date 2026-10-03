package com.example.helpdesk.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketFeedbackResponseDTO {

    private Long id;
    private Long ticketId;
    private Long submittedById;
    private String submittedByName;
    private Integer rating;
    private String comment;
    private OffsetDateTime createdAt;
}




