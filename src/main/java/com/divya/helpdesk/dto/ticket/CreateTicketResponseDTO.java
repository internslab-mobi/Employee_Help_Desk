package com.divya.helpdesk.dto.ticket;

import com.divya.helpdesk.dto.user.DepartmentDTO;
import com.divya.helpdesk.enums.TicketPriority;
import com.divya.helpdesk.enums.TicketStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateTicketResponseDTO {

    private Long ticketId;
    private String ticketNumber;

    private AgentDTO requester;

    private DepartmentDTO department;

    private CategoryDTO category;

    private SubCategoryDTO subCategory;

    private String description;
    private TicketPriority priority;
    private TicketStatus status;

    private AgentDTO agent;

    private AgentDTO manager;

    private Integer reopenCount;
    private String resolutionSummary;
    private String withdrawalReason;

    private OffsetDateTime workStartedAt;
    private OffsetDateTime warningAt;
    private OffsetDateTime resolvedAt;
    private OffsetDateTime withdrawnAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}