package com.example.helpdesk.repository;

import com.example.helpdesk.entity.TicketSla;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TicketSlaRepository extends JpaRepository<TicketSla, Long> {

    TicketSla findByTicketId(Long ticketId);

    List<TicketSla> findByStatusAndCurrentDeadlineAtBefore(String status, LocalDateTime deadline);

    List<TicketSla> findByStatusAndWarningAtBefore(String status, LocalDateTime warningTime);
}
