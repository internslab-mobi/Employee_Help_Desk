package com.example.helpdesk.dto.request;

import com.example.helpdesk.enums.Priority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTicketPriorityRequestDTO {

    @NotNull(message = "Priority is required")
    @Schema(description = "Ticket priority", required = true)
    private Priority priority;
}





