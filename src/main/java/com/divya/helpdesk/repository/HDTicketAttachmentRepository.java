package com.divya.helpdesk.repository;

import com.divya.helpdesk.entity.HDTicketAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HDTicketAttachmentRepository extends JpaRepository<HDTicketAttachment, Long> {

    List<HDTicketAttachment> findByTicketIdOrderByCreatedAtDesc(Long ticketId);
}
