package com.example.helpdesk.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Data for ASSIGN_MANAGER operation")
public class AssignManagerDataRequestDTO implements TicketOperationDataRequestDTO {

    @NotNull(message = "AssignedManagerId is required")
    @Schema(description = "Department manager ID to assign", example = "3", required = true)
    private Long assignedManagerId;
}





