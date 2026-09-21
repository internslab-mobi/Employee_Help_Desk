package xyz.mobi.employeehelpdesk.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import xyz.mobi.employeehelpdesk.entity.enums.HistoryEventType;
import xyz.mobi.employeehelpdesk.entity.enums.TicketStatus;

@Entity
@Table(
        name = "ticket_history",
        indexes = {
                @Index(
                        name = "idx_ticket_history_ticket",
                        columnList = "ticket_id"
                ),
                @Index(
                        name = "idx_ticket_history_event",
                        columnList = "event_type"
                ),
                @Index(
                        name = "idx_ticket_history_created",
                        columnList = "created_at"
                )
        }
)
@Getter
@Setter
public class TicketHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    /*@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private Employee actor;*/

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private HistoryEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true, length = 30)
    private TicketStatus oldValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TicketStatus newValue;

//    @JdbcTypeCode(SqlTypes.JSON)
//    @Column(columnDefinition = "JSON")
//    private String metadata;
}