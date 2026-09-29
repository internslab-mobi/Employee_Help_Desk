package xyz.mobi.employeehelpdesk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.mobi.employeehelpdesk.entity.TicketHistory;

public interface TicketHistoryRepository
        extends JpaRepository<TicketHistory, Long> {

}