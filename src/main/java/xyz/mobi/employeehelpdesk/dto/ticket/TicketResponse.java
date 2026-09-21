package xyz.mobi.employeehelpdesk.dto.ticket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import xyz.mobi.employeehelpdesk.entity.enums.SlaStatus;

import java.time.LocalDateTime;

@Getter
@Builder
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

    private String priority;
    private String status;

    private Long assignedAgentId;
    private String assignedAgentName;

    private Long assignedManagerId;
    private String assignedManagerName;

    private Integer reopenCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime reopenedAt;
    private LocalDateTime assignedAt;

    private SlaStatus slaStatus;
}