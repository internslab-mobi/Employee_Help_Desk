package com.example.helpdesk.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HoldTicketRequestDTO {

    @NotBlank(message = "Hold reason is required")
    @jakarta.validation.constraints.Size(max = 500, message = "Hold reason must not exceed 500 characters")
    private String holdReason;
}
