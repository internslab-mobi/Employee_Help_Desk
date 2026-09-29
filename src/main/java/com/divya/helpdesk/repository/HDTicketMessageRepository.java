package com.divya.helpdesk.repository;

import com.divya.helpdesk.entity.HDTicketMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HDTicketMessageRepository extends JpaRepository<HDTicketMessage, Long> {

    List<HDTicketMessage> findByTicket_IdOrderByCreatedAtAsc(Long ticketId);
}