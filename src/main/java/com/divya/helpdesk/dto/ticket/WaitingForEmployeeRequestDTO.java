package com.divya.helpdesk.dto.ticket;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WaitingForEmployeeRequestDTO {

    @NotBlank(message = "Reason is required")
    private String reason;
}