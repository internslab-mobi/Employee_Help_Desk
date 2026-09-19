package com.example.helpdesk.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReopenTicketRequest {

    @NotBlank(message = "Reopen reason is required")
    @Size(max = 500, message = "Reopen reason must not exceed 500 characters")
    private String reason;
}
