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
@Schema(description = "Data for HOLD operation")
public class HoldData implements TicketOperationData {

    @NotBlank(message = "Hold reason is required")
    @Schema(description = "Reason for putting ticket on hold", example = "Waiting for employee response", required = true)
    private String holdReason;
}
