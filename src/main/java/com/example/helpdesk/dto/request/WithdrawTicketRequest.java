package com.example.helpdesk.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawTicketRequest {

    @NotBlank(message = "Withdrawal reason is required")
    private String withdrawalReason;
}
