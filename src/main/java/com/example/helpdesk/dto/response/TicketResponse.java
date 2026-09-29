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

    private TicketDetailsResponse ticket;
    private EmployeeResponse employee;
    private DepartmentResponse department;
    private CategoryResponse category;
    private AssignmentResponse assignment;
    private SlaResponse sla;
    private List<TicketAttachmentResponse> attachments;
    private OffsetDateTime updatedAt;
}