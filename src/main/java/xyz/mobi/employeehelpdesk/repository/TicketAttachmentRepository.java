package xyz.mobi.employeehelpdesk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.mobi.employeehelpdesk.entity.TicketAttachment;
import xyz.mobi.employeehelpdesk.entity.enums.AttachmentType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TicketAttachmentRepository
        extends JpaRepository<TicketAttachment, Long> {

    List<TicketAttachment> findByMessageIdIn(Collection<Long> messageIds);

    Optional<TicketAttachment> findByEmployeeIdAndAttachmentType(Long employeeId, AttachmentType attachmentType);
}