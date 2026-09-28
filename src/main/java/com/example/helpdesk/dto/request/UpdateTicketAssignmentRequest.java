package com.example.helpdesk.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTicketAssignmentRequest {

    private Long assignedAgentId;

    private Long assignedManagerId;
}
