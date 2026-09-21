package com.example.helpdesk.repository;

import com.example.helpdesk.entity.TicketFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TicketFeedbackRepository extends JpaRepository<TicketFeedback, Long> {

    Optional<TicketFeedback> findByTicketId(Long ticketId);
}
