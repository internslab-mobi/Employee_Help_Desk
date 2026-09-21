package xyz.mobi.employeehelpdesk.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import xyz.mobi.employeehelpdesk.entity.enums.Priority;
import xyz.mobi.employeehelpdesk.entity.enums.TicketStatus;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "tickets",
        indexes = {
                @Index(name = "idx_ticket_requester", columnList = "requester_id"),
                @Index(name = "idx_ticket_department", columnList = "department_id"),
                @Index(name = "idx_ticket_category", columnList = "category_id"),
                @Index(name = "idx_ticket_sub_category", columnList = "sub_category_id"),
                @Index(name = "idx_ticket_assigned_agent", columnList = "assigned_agent_id"),
                @Index(name = "idx_ticket_status", columnList = "status"),
                @Index(name = "idx_ticket_priority", columnList = "priority")
        }
)
@Getter
@Setter
@Builder(toBuilder = true)
@RequiredArgsConstructor
@AllArgsConstructor
public class Ticket extends BaseEntity {

    @Column(nullable = true, unique = true, length = 50)
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

    private LocalDateTime assignedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_manager_id")
    private DepartmentManager assignedManager;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sla_policy_id")
    private SlaPolicy slaPolicy;

    @Column(nullable = false)
    private Integer reopenCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Priority priority;

    @Column(columnDefinition = "TEXT")
    private String resolutionSummary;

    @Column(columnDefinition = "TEXT")
    private String holdReason;

    private LocalDateTime holdStartedAt;

    @Column(columnDefinition = "TEXT")
    @Size(min = 1, max = 100)
    private String withdrawalReason;

    private LocalDateTime resolvedAt;

    private LocalDateTime reopenedAt;

    private LocalDateTime withdrawnAt;

}