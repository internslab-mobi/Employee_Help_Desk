package com.divya.helpdesk.entity;

import com.divya.helpdesk.enums.HDPriorityLevel;
import com.divya.helpdesk.enums.HDTicketStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "hd_tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HDTicket extends BaseEntity {

    @Column(name = "ticket_number", nullable = false, unique = true, length = 50)
    private String ticketNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private HDEmployee requester;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private HDDepartment department;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private HDCategory category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sub_category_id", nullable = false)
    private HDSubCategory subCategory;

    @Column(nullable = false, length = 255)
    private String subject;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private HDTicketStatus status = HDTicketStatus.NEW;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_agent_id")
    private HDEmployee assignedAgent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_manager_id")
    private HDEmployee assignedManager;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sla_policy_id")
    private HDSlaPolicy slaPolicy;

    @Column(name = "reopen_count", nullable = false)
    private Integer reopenCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private HDPriorityLevel priority = HDPriorityLevel.MEDIUM;

    @Column(name = "resolution_summary", columnDefinition = "TEXT")
    private String resolutionSummary;

    @Column(name = "hold_reason", columnDefinition = "TEXT")
    private String holdReason;

    @Column(name = "hold_started_at")
    private LocalDateTime holdStartedAt;

    @Column(name = "withdrawal_reason", columnDefinition = "TEXT")
    private String withdrawalReason;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;
}
