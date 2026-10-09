package com.example.helpdesk.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketMessageRequestDTO {

    @NotNull(message = "Sender ID is required")
    private Long senderId;

    @NotBlank(message = "Content is required")
    private String content;
}





