package xyz.mobi.employeehelpdesk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.mobi.employeehelpdesk.entity.TicketFeedback;

import java.util.Optional;

public interface TicketFeedbackRepository
        extends JpaRepository<TicketFeedback, Long> {

    Optional<TicketFeedback> findByTicketId(Long ticketId);

    boolean existsByTicketId(Long ticketId);
}