package com.example.helpdesk.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TicketMessageRequest {

    @NotBlank(message = "Sender ID is required")
    private Long senderId;

    @NotBlank(message = "Content is required")
    private String content;
}
