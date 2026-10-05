package com.divya.helpdesk.dto.ticket;

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

    private Long departmentId;
    private Long categoryId;
    private Long subCategoryId;
    private String description;
}
