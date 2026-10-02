package com.divya.helpdesk.dto.ticket;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequestDTO {

    @NotBlank(message = "Message is required")
    private String message;
}