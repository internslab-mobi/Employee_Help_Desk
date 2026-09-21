package xyz.mobi.employeehelpdesk.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import xyz.mobi.employeehelpdesk.entity.TicketMessage;

import java.util.List;

public interface TicketMessageRepository
        extends JpaRepository<TicketMessage, Long> {

    List<TicketMessage> findByTicketIdOrderByCreatedAtAsc(
            Long ticketId
    );

    Page<TicketMessage> findByTicketIdOrderByCreatedAtAsc(
            Long ticketId,
            Pageable pageable
    );
}