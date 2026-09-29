package com.divya.helpdesk.entity;

import com.divya.helpdesk.enums.SlaInstanceStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(
        name = "hd_sla_instances",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_sla_instance_ticket",
                        columnNames = "ticket_id"
                )
        }
)
public class HDSlaInstance extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "ticket_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_sla_instance_ticket")
    )
    private HDTicket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "sla_policy_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_sla_instance_policy")
    )
    private HDSlaPolicy slaPolicy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SlaInstanceStatus status;

    @Column(name = "allocated_minutes", nullable = false)
    private Integer allocatedMinutes;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "original_deadline_at", nullable = false)
    private Instant originalDeadlineAt;

    @Column(name = "current_deadline_at", nullable = false)
    private Instant currentDeadlineAt;

    @Column(name = "warning_at")
    private Instant warningAt;

    @Column(name = "warning_sent_at")
    private Instant warningSentAt;

    @Column(name = "breached_at")
    private Instant breachedAt;

    @Column(name = "breach_sent_at")
    private Instant breachSentAt;

    @Column(name = "cycle_number", nullable = false)
    private Integer cycleNumber;
}