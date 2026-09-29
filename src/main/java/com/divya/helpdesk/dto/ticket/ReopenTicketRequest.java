package com.divya.helpdesk.dto.ticket;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReopenTicketRequest {

    @NotBlank(message = "Reopen reason is required")
    private String reason;
}