package xyz.mobi.employeehelpdesk.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import xyz.mobi.employeehelpdesk.entity.enums.Priority;
import xyz.mobi.employeehelpdesk.entity.enums.TicketStatus;

import java.time.Instant;

@Entity
@Table(
        name = "tickets",
        indexes = {
                @Index(name = "idx_ticket_requester", columnList = "requester_id"),
                @Index(name = "idx_ticket_requester_created", columnList = "requester_id, created_at"),
                @Index(name = "idx_ticket_req_status_created", columnList = "requester_id, status, created_at"),
                @Index(name = "idx_ticket_department", columnList = "department_id"),
                @Index(name = "idx_ticket_category", columnList = "category_id"),
                @Index(name = "idx_ticket_sub_category", columnList = "sub_category_id"),
                @Index(name = "idx_ticket_assigned_agent", columnList = "assigned_agent_id"),
                @Index(name = "idx_ticket_agent_created", columnList = "assigned_agent_id, created_at"),
                @Index(name = "idx_ticket_agent_status_created", columnList = "assigned_agent_id, status, created_at"),
                @Index(name = "idx_ticket_agent_status", columnList = "assigned_agent_id, status"),
                @Index(name = "idx_ticket_status", columnList = "status"),
                @Index(name = "idx_ticket_priority", columnList = "priority"),
                @Index(name = "idx_ticket_number", columnList = "ticketNumber")
        }
)
@Getter
@Setter
@Builder(toBuilder = true)
@RequiredArgsConstructor
@AllArgsConstructor
public class Ticket extends BaseEntity {

    @Column(unique = true, length = 50)
    private String ticketNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private Employee requester;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_category_id")
    private SubCategory subCategory;

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TicketStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_agent_id")
    private DepartmentAgent assignedAgent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private DepartmentManager assignedManager;

    private Instant assignedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sla_policy_id")
    private SlaPolicy slaPolicy;

    @Builder.Default
    @Column(nullable = false)
    private Integer reopenCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Priority priority;

    @Column(columnDefinition = "TEXT")
    private String resolutionSummary;

    @Column(columnDefinition = "TEXT")
    private String holdReason;

    private Instant holdStartedAt;

    @Column(columnDefinition = "TEXT")
    @Size(min = 1, max = 100)
    private String withdrawalReason;

    @Column(columnDefinition = "TEXT")
    @Size(min = 1, max = 100)
    private String reopenReason;

    private Instant resolvedAt;

    private Instant reopenedAt;

    private Instant withdrawnAt;

    @Version
    private Long version;

}