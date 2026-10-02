package com.divya.helpdesk.repository;

import com.divya.helpdesk.entity.HDTicketFeedbackEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HDTicketFeedbackRepository extends JpaRepository<HDTicketFeedbackEntity, Long> {

    boolean existsByTicketId(Long ticketId);
}
