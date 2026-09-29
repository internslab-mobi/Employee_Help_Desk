package com.divya.helpdesk.dto.ticket;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResolveTicketRequest {

    @NotBlank(message = "Resolution summary is required")
    private String resolutionSummary;
}