package com.example.helpdesk.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@Schema(description = "Data for RESUME operation (no additional data required)")
public class ResumeData implements TicketOperationData {
}
