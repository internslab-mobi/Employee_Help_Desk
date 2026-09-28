package com.example.helpdesk.dto.request;

import com.example.helpdesk.enums.Priority;
import com.example.helpdesk.enums.TicketPatchOperation;
import com.example.helpdesk.enums.TicketStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTicketRequest {

    @NotNull(message = "Operation is required")
    private TicketPatchOperation operation;

    private TicketStatus status;

    private Priority priority;

    private Long categoryId;

    private Long subCategoryId;

    private Long assignedAgentId;

    private Long assignedManagerId;

    @Size(max = 500, message = "Hold reason must not exceed 500 characters")
    private String holdReason;

    @Size(max = 1000, message = "Resolution summary must not exceed 1000 characters")
    private String resolutionSummary;

    @Size(max = 500, message = "Withdrawal reason must not exceed 500 characters")
    private String withdrawalReason;
}
