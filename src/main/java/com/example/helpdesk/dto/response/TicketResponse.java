package com.example.helpdesk.dto.response;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketResponse {

    private Long id;

    private String ticketNumber;

    private Long requesterId;
    private String requesterName;

    private Long departmentId;
    private String departmentName;

    private Long categoryId;
    private String categoryName;

    private Long subCategoryId;
    private String subCategoryName;

    private String subject;

    private String description;

    private String priority;

    private String status;

    private Long assignedAgentId;
    private String assignedAgentName;

    private Long assignedManagerId;
    private String assignedManagerName;

    private Long slaPolicyId;

    private Integer reopenCount;

    private String resolutionSummary;

    private String holdReason;

    private OffsetDateTime holdStartedAt;

    private String withdrawalReason;

    private OffsetDateTime createdAt;
    private OffsetDateTime resolvedAt;
    private OffsetDateTime closedAt;
    private OffsetDateTime withdrawnAt;
    private OffsetDateTime updatedAt;

    private List<TicketAttachmentResponse> attachments;
}