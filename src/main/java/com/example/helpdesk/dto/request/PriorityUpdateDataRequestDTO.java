package com.example.helpdesk.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Data for PRIORITY operation")
public class PriorityUpdateDataRequestDTO implements TicketOperationDataRequestDTO {

    @NotBlank(message = "Priority is required")
    @Schema(description = "New ticket priority", example = "HIGH", required = true)
    private String priority;
}





