package com.example.helpdesk.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResolveTicketRequest {

    @NotBlank(message = "Resolution summary is required")
    private String resolutionSummary;
}
