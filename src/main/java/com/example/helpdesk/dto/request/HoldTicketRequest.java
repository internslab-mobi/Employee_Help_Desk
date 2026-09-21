package com.example.helpdesk.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class HoldTicketRequest {

    @NotBlank(message = "Reason is required")
    private String reason;
}
