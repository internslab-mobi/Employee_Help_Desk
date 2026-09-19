package com.divya.helpdesk.dto.response;

import com.divya.helpdesk.enums.HDSlaStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SlaInstanceResponse {
    private Long id;
    private Long ticketId;
    private Long slaPolicyId;
    private Integer cycleNumber;
    private Integer allocatedMinutes;
    private LocalDateTime slaStartAt;
    private LocalDateTime originalDeadlineAt;
    private LocalDateTime currentDeadlineAt;
    private LocalDateTime warningAt;
    private HDSlaStatus status;
    private LocalDateTime pausedAt;
    private LocalDateTime breachedAt;
    private LocalDateTime createdAt;
}
