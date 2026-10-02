package com.divya.helpdesk.repository;

import com.divya.helpdesk.entity.HDTicketMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HDTicketMessageRepository extends JpaRepository<HDTicketMessageEntity, Long> {

    List<HDTicketMessageEntity> findByTicketIdOrderByCreatedAtAsc(Long ticketId);
}