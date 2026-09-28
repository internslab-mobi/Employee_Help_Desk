package com.example.helpdesk.dto.request;

import com.example.helpdesk.enums.TicketStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTicketStatusRequest {

    @NotNull(message = "Status is required")
    @Schema(description = "Ticket status", required = true)
    private TicketStatus status;

    private String resolutionSummary;
}
