package com.divya.helpdesk.dto.ticket;

import com.divya.helpdesk.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TicketFilterRequest {

    private TicketStatus status;

    private Long agentId;

    private Long departmentId;

    private Long categoryId;

    private Long subCategoryId;
}