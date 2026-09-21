package com.example.helpdesk.repository;

import com.example.helpdesk.entity.TicketMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketMessageRepository extends JpaRepository<TicketMessage, Long> {

    List<TicketMessage> findByTicketIdOrderByCreatedAtAsc(Long ticketId);

    List<TicketMessage> findByTicketIdAndSeenFalseOrderByCreatedAtAsc(Long ticketId);

    long countByTicketIdAndSeenFalse(Long ticketId);
}
