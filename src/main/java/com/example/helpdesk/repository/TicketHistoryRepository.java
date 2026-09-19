package com.example.helpdesk.repository;

import com.example.helpdesk.entity.TicketHistory;
import com.example.helpdesk.enums.TicketEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketHistoryRepository extends JpaRepository<TicketHistory, Long> {
    List<TicketHistory> findByTicketIdOrderByCreatedAtDesc(Long ticketId);
    Optional<TicketHistory> findFirstByTicketIdOrderByCreatedAtDesc(Long ticketId);
    boolean existsByTicketIdAndEventType(Long ticketId, TicketEventType eventType);
}
