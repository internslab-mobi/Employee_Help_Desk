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
public class SlaResponse {
    
    private Long ticketId;
    private String ticketNumber;
    private Long slaPolicyId;
    private Integer cycleNumber;
    private Integer allocatedMinutes;
    private LocalDateTime slaStartAt;
    private LocalDateTime warningAt;
    private LocalDateTime deadlineAt;
    private String status;
    private LocalDateTime breachedAt;
    private Integer remainingMinutes;
}
