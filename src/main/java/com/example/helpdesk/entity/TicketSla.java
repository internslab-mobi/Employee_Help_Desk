package com.example.helpdesk.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "hd_sla_instances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketSla {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sla_policy_id", nullable = false)
    private SlaRule slaPolicy;

    @Builder.Default
    @Column(name = "cycle_number", nullable = false)
    private Integer cycleNumber = 0;

    @Column(name = "allocated_minutes", nullable = false)
    private Integer allocatedMinutes;

    @Column(name = "sla_start_at", nullable = false)
    private Instant slaStartAt;

    @Column(name = "original_deadline_at", nullable = false)
    private Instant originalDeadlineAt;

    @Column(name = "current_deadline_at", nullable = false)
    private Instant currentDeadlineAt;

    @Column(name = "warning_at")
    private Instant warningAt;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "paused_at")
    private Instant pausedAt;

    @Column(name = "breached_at")
    private Instant breachedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}