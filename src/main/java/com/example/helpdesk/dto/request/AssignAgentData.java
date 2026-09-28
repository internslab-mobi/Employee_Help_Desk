package com.example.helpdesk.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Data for ASSIGN_AGENT operation")
public class AssignAgentData implements TicketOperationData {

    @NotNull(message = "AgentId is required")
    @Schema(description = "Agent employee ID to assign", example = "5", required = true)
    private Long agentId;
}
