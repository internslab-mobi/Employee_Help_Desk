package com.example.helpdesk.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketDetailsResponse {

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
}
