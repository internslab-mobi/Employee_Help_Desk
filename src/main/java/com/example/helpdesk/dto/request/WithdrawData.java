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
@Schema(description = "Data for WITHDRAW operation")
public class WithdrawData implements TicketOperationData {

    @NotBlank(message = "Withdrawal reason is required")
    @Schema(description = "Reason for withdrawing the ticket", example = "Issue is no longer required", required = true)
    private String withdrawalReason;
}
