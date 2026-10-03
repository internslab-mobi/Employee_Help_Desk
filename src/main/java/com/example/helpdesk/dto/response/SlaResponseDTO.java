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
public class SlaResponseDTO {
    
    private Long ticketId;
    private String ticketNumber;
    private Long slaPolicyId;
    private Integer cycleNumber;
    private Integer allocatedMinutes;
    private OffsetDateTime slaStartAt;
    private OffsetDateTime warningAt;
    private OffsetDateTime deadlineAt;
    private String status;
    private OffsetDateTime breachedAt;
    private Integer remainingMinutes;
}




