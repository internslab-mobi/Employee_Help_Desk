package xyz.mobi.employeehelpdesk.entity;

import jakarta.persistence.*;
import lombok.*;
import xyz.mobi.employeehelpdesk.entity.enums.SlaEventType;
import xyz.mobi.employeehelpdesk.entity.enums.SlaStatus;

import java.time.Instant;

@Entity
@Table(
        name = "hd_sla_instances",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_sla_instance_ticket_cycle",
                        columnNames = {"ticket_id", "cycle_number"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_sla_instance_ticket",
                        columnList = "ticket_id"
                ),
                @Index(
                        name = "idx_sla_instance_policy",
                        columnList = "sla_policy_id"
                ),
                @Index(
                        name = "idx_sla_instance_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_sla_pending_events",
                        columnList = "status, next_event_type, next_event_at"
                )
        }
)
@Getter
@Setter
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class SlaInstance extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sla_policy_id", nullable = false)
    private SlaPolicy slaPolicy;

    @Builder.Default
    @Column(nullable = false)
    private Integer cycleNumber = 0;

    @Column(nullable = false)
    private Integer allocatedMinutes;

    @Column(nullable = false)
    private Instant slaStartAt;

    @Column(nullable = false)
    private Instant originalDeadlineAt;

    @Column(nullable = false)
    private Instant currentDeadlineAt;

    private Instant warningAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SlaStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private SlaEventType nextEventType;

    private Instant nextEventAt;

    private Instant pausedAt;

    private Instant breachedAt;
}