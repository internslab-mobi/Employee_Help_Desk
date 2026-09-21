package xyz.mobi.employeehelpdesk.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ticket_agent_history")
@Getter
@Setter
public class TicketAgentHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_agent_id")
    private DepartmentAgent previousAgent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_agent_id", nullable = false)
    private DepartmentAgent newAgent;
}
