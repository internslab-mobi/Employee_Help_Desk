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
        description = "Consolidated ticket update request. Use the 'operation' field to specify the type of update, and provide operation-specific data in the 'data' field."
)
public class UpdateTicketRequestDTO {

    @NotNull(message = "Operation is required")
    @Schema(
            description = "Select the ticket update operation: STATUS, PRIORITY, CATEGORY, ASSIGN_AGENT, ASSIGN_MANAGER, HOLD, RESUME, RESOLVE, REOPEN, WITHDRAW",
            required = true,
            allowableValues = {"STATUS", "PRIORITY", "CATEGORY", "ASSIGN_AGENT", "ASSIGN_MANAGER", "HOLD", "RESUME", "RESOLVE", "REOPEN", "WITHDRAW"}
    )
    private TicketPatchOperation operation;

    @NotNull(message = "Data is required")
    @Schema(
            description = "Operation-specific data. The fields depend on the selected operation. See operation-specific request DTOs for schema details.",
            required = true
    )
    private JsonNode data;
}





