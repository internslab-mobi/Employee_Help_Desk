package xyz.mobi.employeehelpdesk.dto.ticket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import xyz.mobi.employeehelpdesk.entity.enums.SlaStatus;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
public class TicketUpdateResponse {

    private Long id;
    private String ticketNumber;

    private IdNameResponse requester;
    private IdNameResponse department;
    private IdNameResponse category;
    private IdNameResponse subCategory;

    private String subject;
    private String description;

    private String priority;
    private String status;

    private IdNameResponse assignedAgent;
    private Long managerId;

    private Integer reopenCount;

    private Instant updatedAt;
    private Instant resolvedAt;
    private Instant reopenedAt;
    private Instant assignedAt;

    private SlaStatus slaStatus;
}
