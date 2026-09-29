package com.divya.helpdesk.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "hd_ticket_feedback",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_ticket_feedback_ticket",
                        columnNames = "ticket_id"
                )
        }
)
public class HDTicketFeedback extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "ticket_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_feedback_ticket")
    )
    private HDTicket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "submitted_by",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_feedback_employee")
    )
    private HDEmployee submittedBy;

    @Column(name = "rating", nullable = false)
    private Integer rating;
}