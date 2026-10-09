package xyz.mobi.employeehelpdesk.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import xyz.mobi.employeehelpdesk.entity.TicketMessage;

public interface TicketMessageRepository
        extends JpaRepository<TicketMessage, Long> {

    @EntityGraph(attributePaths = {"sender", "ticket"})
    Page<TicketMessage> findByTicketIdOrderByCreatedAtAsc(
            Long ticketId,
            Pageable pageable
    );
}