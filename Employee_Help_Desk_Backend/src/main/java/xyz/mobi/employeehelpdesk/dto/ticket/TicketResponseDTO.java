package xyz.mobi.employeehelpdesk.dto.ticket;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import xyz.mobi.employeehelpdesk.entity.enums.SlaStatus;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TicketResponseDTO {

    private Long id;
    private String ticketNumber;

    private IdNameResponseDTO requester;
    private IdNameResponseDTO department;
    private IdNameResponseDTO category;
    private IdNameResponseDTO subCategory;

    private String subject;
    private String description;

    private String priority;
    private String status;

    private IdNameResponseDTO assignedAgent;
    private Long managerId;

    private Integer reopenCount;

    private Instant createdAt;
    private Instant updatedAt;
    private Instant resolvedAt;
    private Instant reopenedAt;
    private Instant assignedAt;

    private SlaStatus slaStatus;
    private Instant slaDeadline;
}