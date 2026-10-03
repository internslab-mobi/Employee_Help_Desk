package com.example.helpdesk.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignManagerRequestDTO {

    @NotNull(message = "AssignedManagerId is required")
    private Long assignedManagerId;
}
