package com.divya.helpdesk.repository;

import com.divya.helpdesk.entity.HDTicketHistoryEntity;
import com.divya.helpdesk.enums.TicketEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HDTicketHistoryRepository extends JpaRepository<HDTicketHistoryEntity, Long> {

    List<HDTicketHistoryEntity> findByTicketIdOrderByCreatedAtAsc(Long ticketId);

    Optional<HDTicketHistoryEntity> findTopByNewValueAndEventTypeOrderByCreatedAtDesc(
            String newValue,
            TicketEventType eventType
    );
}