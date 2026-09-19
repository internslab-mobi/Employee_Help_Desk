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
public class TicketDetailsResponse {
    private Long id;
    private String ticketNumber;
    private EmployeeResponse requester;
    private DepartmentResponse department;
    private CategoryResponse category;
    private SubCategoryResponse subCategory;
    private String subject;
    private String description;
    private HDTicketStatus status;
    private EmployeeResponse assignedAgent;
    private EmployeeResponse assignedManager;
    private SlaPolicyResponse slaPolicy;
    private SlaInstanceResponse slaInstance;
    private HDPriorityLevel priority;
    private Integer reopenCount;
    private String resolutionSummary;
    private String holdReason;
    private LocalDateTime holdStartedAt;
    private String withdrawalReason;
    private LocalDateTime resolvedAt;
    private LocalDateTime closedAt;
    private LocalDateTime withdrawnAt;
    private LocalDateTime dueAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
