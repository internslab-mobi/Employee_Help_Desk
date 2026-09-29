package com.divya.helpdesk.repository;

import com.divya.helpdesk.entity.HDTicketAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HDTicketAttachmentRepository extends JpaRepository<HDTicketAttachment, Long> {

    List<HDTicketAttachment> findByTicket_IdOrderByCreatedAtDesc(Long ticketId);
    Optional<HDTicketAttachment> findByIdAndTicket_Id(Long id, Long ticketId);
}

