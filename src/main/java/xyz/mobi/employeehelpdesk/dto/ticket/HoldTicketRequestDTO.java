package xyz.mobi.employeehelpdesk.dto.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HoldTicketRequestDto(
        @NotBlank(message = "Hold reason is required")
        @Size(min = 1, max = 100, message = "Reason must be between 1 and 100 characters")
        String reason
) {
}
