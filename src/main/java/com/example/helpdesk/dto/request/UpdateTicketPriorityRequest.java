package com.example.helpdesk.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTicketPriorityRequest {

    @NotNull(message = "Priority is required")
    @Size(max = 30, message = "Priority must not exceed 30 characters")
    private String priority;
}
