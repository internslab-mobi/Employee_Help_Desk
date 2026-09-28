package com.example.helpdesk.dto.request;

import com.example.helpdesk.enums.TicketPatchOperation;
import com.fasterxml.jackson.databind.JsonNode;
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
@Schema(
        description = "Wrapper DTO for consolidated ticket update operations."
)
public class UpdateDTO {

    @NotNull(message = "Operation is required")
    @Schema(
            description = "Select the ticket update operation: STATUS, PRIORITY, CATEGORY, ASSIGN_AGENT, ASSIGN_MANAGER, HOLD, RESUME, RESOLVE, REOPEN, WITHDRAW",
            required = true
    )
    private TicketPatchOperation operation;

    @NotNull(message = "Data is required")
    @Schema(
            description = "Operation-specific data. The fields depend on the selected operation. See operation-specific request DTOs for schema details.",
            required = true
    )
    private JsonNode data;
}