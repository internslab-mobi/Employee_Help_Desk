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
public class TicketDetailsResponseDTO {

    private Long id;
    private String ticketNumber;
    private String subject;
    private String description;
    private String priority;
    private String status;
    private Integer reopenCount;
    private String resolutionSummary;
    private String holdReason;
    private String withdrawalReason;
    private OffsetDateTime createdAt;
    private OffsetDateTime resolvedAt;
    private OffsetDateTime closedAt;
    private OffsetDateTime withdrawnAt;
    private OffsetDateTime holdStartedAt;
}




