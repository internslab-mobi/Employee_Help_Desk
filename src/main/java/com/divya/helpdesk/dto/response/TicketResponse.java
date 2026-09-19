package com.divya.helpdesk.dto.response;

import com.divya.helpdesk.enums.HDPriorityLevel;
import com.divya.helpdesk.enums.HDTicketStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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
    private HDTicketStatus status;
    private Long assignedAgentId;
    private String assignedAgentName;
    private Long assignedManagerId;
    private String assignedManagerName;
    private HDPriorityLevel priority;
    private Integer reopenCount;
    private String resolutionSummary;
    private LocalDateTime resolvedAt;
    private LocalDateTime closedAt;
    private LocalDateTime withdrawnAt;
    private LocalDateTime dueAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
