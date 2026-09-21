package xyz.mobi.employeehelpdesk.entity;

import jakarta.persistence.*;
import lombok.*;
import xyz.mobi.employeehelpdesk.entity.enums.SlaStatus;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "sla_instances",
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
                        name = "idx_sla_breach_candidates",
                        columnList = "status, current_deadline_at"
                ),
                @Index(
                        name = "idx_sla_warning_candidates",
                        columnList = "status, warning_at, current_deadline_at"
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

    @Column(nullable = false)
    private Integer cycleNumber = 0;

    @Column(nullable = false)
    private Integer allocatedMinutes;

    @Column(nullable = false)
    private LocalDateTime slaStartAt;

    @Column(nullable = false)
    private LocalDateTime originalDeadlineAt;

    @Column(nullable = false)
    private LocalDateTime currentDeadlineAt;

    private LocalDateTime warningAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SlaStatus status;

    private LocalDateTime pausedAt;

    /*@Column(nullable = false)
    private Integer totalPausedMinutes = 0;*/

    private LocalDateTime breachedAt;
}