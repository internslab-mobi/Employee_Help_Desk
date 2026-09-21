package xyz.mobi.employeehelpdesk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.mobi.employeehelpdesk.entity.enums.HistoryEventType;
import xyz.mobi.employeehelpdesk.entity.TicketHistory;

import java.util.List;

public interface TicketHistoryRepository
        extends JpaRepository<TicketHistory, Long> {

    List<TicketHistory> findByTicketIdOrderByCreatedAtAsc(
            Long ticketId
    );

    List<TicketHistory> findByTicketIdAndEventType(
            Long ticketId,
            HistoryEventType eventType
    );

    TicketHistory findByTicketId(long ticketId);
}