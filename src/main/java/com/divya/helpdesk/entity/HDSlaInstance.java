package com.divya.helpdesk.entity;

import com.divya.helpdesk.enums.HDSlaStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "hd_sla_instances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HDSlaInstance extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private HDTicket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sla_policy_id", nullable = false)
    private HDSlaPolicy slaPolicy;

    @Column(name = "cycle_number", nullable = false)
    private Integer cycleNumber = 1;

    @Column(name = "allocated_minutes", nullable = false)
    private Integer allocatedMinutes;

    @Column(name = "sla_start_at", nullable = false)
    private LocalDateTime slaStartAt;

    @Column(name = "original_deadline_at", nullable = false)
    private LocalDateTime originalDeadlineAt;

    @Column(name = "current_deadline_at", nullable = false)
    private LocalDateTime currentDeadlineAt;

    @Column(name = "warning_at")
    private LocalDateTime warningAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private HDSlaStatus status = HDSlaStatus.ACTIVE;

    @Column(name = "paused_at")
    private LocalDateTime pausedAt;

    @Column(name = "breached_at")
    private LocalDateTime breachedAt;

    @Column(name = "warning_sent_at")
    private LocalDateTime warningSentAt;

    @Column(name = "breach_sent_at")
    private LocalDateTime breachSentAt;
}
