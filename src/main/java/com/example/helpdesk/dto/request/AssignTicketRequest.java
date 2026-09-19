package com.example.helpdesk.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignTicketRequest {

    @NotNull(message = "Agent ID is required")
    private Long agentId;

    @NotNull(message = "Assigned by employee ID is required")
    private Long assignedBy;
}
