package com.divya.helpdesk.dto.ticket;

import com.divya.helpdesk.enums.TicketPriority;
import com.divya.helpdesk.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload for flexible single PATCH endpoint on Ticket.
 *
 * Only supplied non-null fields will be updated.
 * System-controlled fields (id, ticketNumber, requesterId, createdAt, updatedAt) cannot be patched.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketPatchRequestDTO {

    private TicketStatus status;
    private TicketPriority priority;
    private String description;
    private Long assignedAgentId;
    private Long assignedManagerId;
    private String resolutionSummary;
    private String holdReason;
    private String withdrawalReason;
}
