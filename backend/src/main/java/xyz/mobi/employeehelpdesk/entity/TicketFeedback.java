package xyz.mobi.employeehelpdesk.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "hd_ticket_feedback",
        indexes = {
                @Index(
                        name = "idx_ticket_feedback_ticket",
                        columnList = "ticket_id"
                )
        }
)
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TicketFeedback extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "ticket_id",
            nullable = false,
            unique = true
    )
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submitted_by", nullable = false)
    private Employee submittedBy;

    @Column(nullable = false)
    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String comment;
}