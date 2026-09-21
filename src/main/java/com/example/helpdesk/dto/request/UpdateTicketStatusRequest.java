package com.example.helpdesk.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTicketStatusRequest {

    @NotNull(message = "Status is required")
    @Size(max = 30, message = "Status must not exceed 30 characters")
    private String status;

    private String resolutionSummary;
}
