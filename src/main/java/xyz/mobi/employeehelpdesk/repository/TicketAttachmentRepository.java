package xyz.mobi.employeehelpdesk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.mobi.employeehelpdesk.entity.TicketAttachment;

import java.util.Collection;
import java.util.List;

public interface TicketAttachmentRepository
        extends JpaRepository<TicketAttachment, Long> {

    List<TicketAttachment> findByTicketId(Long ticketId);

    List<TicketAttachment> findByMessageId(Long messageId);

    List<TicketAttachment> findByMessageIdIn(Collection<Long> messageIds);

    List<TicketAttachment> findByTicketIdAndMessageIsNull(Long ticketId);
}