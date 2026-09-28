package com.example.helpdesk.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Data for STATUS operation")
public class StatusUpdateData implements TicketOperationData {

    @NotBlank(message = "Status is required")
    @Schema(description = "New ticket status", example = "IN_PROGRESS", required = true)
    private String status;
}
