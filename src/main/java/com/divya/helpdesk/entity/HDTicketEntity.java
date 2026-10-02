package com.divya.helpdesk.entity;

import com.divya.helpdesk.enums.TicketPriority;
import com.divya.helpdesk.enums.TicketStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(
        name = "hd_tickets",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_ticket_number",
                        columnNames = "ticket_number"
                )
        }
)
public class HDTicketEntity extends BaseEntity {

    @Column(name = "ticket_number", nullable = false, length = 50)
    private String ticketNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "requester_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ticket_requester")
    )
    private HDEmployeeEntity requester;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "department_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ticket_department")
    )
    private HDDepartmentEntity department;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "category_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ticket_category")
    )
    private HDCategoryEntity category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "sub_category_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ticket_sub_category")
    )
    private HDSubCategoryEntity subCategory;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TicketStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 30)
    private TicketPriority priority;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "assigned_agent_id",
            foreignKey = @ForeignKey(name = "fk_ticket_assigned_agent")
    )
    private HDEmployeeEntity assignedAgent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "assigned_manager_id",
            foreignKey = @ForeignKey(name = "fk_ticket_assigned_manager")
    )
    private HDEmployeeEntity assignedManager;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "sla_policy_id",
            foreignKey = @ForeignKey(name = "fk_ticket_sla_policy")
    )
    private HDSlaPolicyEntity slaPolicy;

    @Column(name = "reopen_count", nullable = false)
    private Integer reopenCount;

    @Column(name = "resolution_summary", columnDefinition = "TEXT")
    private String resolutionSummary;

    @Column(name = "hold_reason", columnDefinition = "TEXT")
    private String holdReason;

    @Column(name = "hold_started_at")
    private Instant holdStartedAt;

    @Column(name = "withdrawal_reason", columnDefinition = "TEXT")
    private String withdrawalReason;

    @Column(name = "work_started_at")
    private Instant workStartedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "withdrawn_at")
    private Instant withdrawnAt;
}