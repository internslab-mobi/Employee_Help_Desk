package com.example.helpdesk.dto.response;

import com.example.helpdesk.enums.Priority;
import com.example.helpdesk.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponseDTO {

    private Long id;
    private String ticketNumber;
    private RequesterResponse requester;
    private DepartmentResponse department;
    private CategoryResponse category;
    private SubCategoryResponse subCategory;
    private String subject;
    private String description;
    private Priority priority;
    private TicketStatus status;
    private AssignedAgentResponse assignedAgent;
    private Long managerId;
    private Integer reopenCount;
    private OffsetDateTime createdAt;
    private OffsetDateTime resolvedAt;
    private OffsetDateTime reopenedAt;
    private OffsetDateTime assignedAt;
    private String slaStatus;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequesterResponse {
        private Long id;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepartmentResponse {
        private Long id;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryResponse {
        private Long id;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubCategoryResponse {
        private Long id;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignedAgentResponse {
        private Long id;
        private String name;
    }
}
