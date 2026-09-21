package xyz.mobi.employeehelpdesk.dto.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResolveTicketRequestDto(
        @NotBlank(message = "Resolution summary is required")
        @Size(min = 1, max = 500, message = "Resolution summary must be between 1 and 500 characters")
        String resolutionSummary
) {
}
