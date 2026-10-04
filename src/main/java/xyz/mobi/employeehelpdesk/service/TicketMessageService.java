package xyz.mobi.employeehelpdesk.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import xyz.mobi.employeehelpdesk.dto.message.TicketMessageCreateResponseDTO;
import xyz.mobi.employeehelpdesk.dto.message.TicketMessageResponseDTO;
import xyz.mobi.employeehelpdesk.entity.TicketAttachment;

import java.io.IOException;
import java.util.List;

public interface TicketMessageService {

    TicketMessageCreateResponseDTO createMessage(
            Long ticketId,
            String content,
            List<MultipartFile> attachments
    ) throws IOException;

    Page<TicketMessageResponseDTO> getMessages(
            Long ticketId,
            Pageable pageable
    );

    TicketAttachment getAttachmentForDownload(
            Long ticketId,
            Long attachmentId
    );
}
