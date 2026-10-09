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
@Schema(description = "Data for RESOLVE operation")
public class ResolveDataRequestDTO implements TicketOperationDataRequestDTO {

    @NotBlank(message = "Resolution summary is required")
    @Schema(description = "Summary of how the ticket was resolved", example = "Laptop issue resolved successfully", required = true)
    private String resolutionSummary;
}





