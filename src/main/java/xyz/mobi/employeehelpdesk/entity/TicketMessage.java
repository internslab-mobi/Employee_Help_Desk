package xyz.mobi.employeehelpdesk.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "ticket_messages",
        indexes = {
                @Index(
                        name = "idx_ticket_message_ticket",
                        columnList = "ticket_id"
                ),
                @Index(
                        name = "idx_ticket_message_sender",
                        columnList = "sender_id"
                ),
                @Index(
                        name = "idx_ticket_message_created",
                        columnList = "created_at"
                )
        }
)
@Getter
@Setter
public class TicketMessage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private Employee sender;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private Boolean seen = false;
}