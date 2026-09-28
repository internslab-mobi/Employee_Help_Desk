package com.example.helpdesk.repository;

import com.example.helpdesk.entity.TicketSla;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface TicketSlaRepository extends JpaRepository<TicketSla, Long> {

    TicketSla findByTicketId(Long ticketId);

    List<TicketSla> findByStatusAndCurrentDeadlineAtBefore(String status, Instant deadline);

    List<TicketSla> findByStatusAndWarningAtBefore(String status, Instant warningTime);
}
