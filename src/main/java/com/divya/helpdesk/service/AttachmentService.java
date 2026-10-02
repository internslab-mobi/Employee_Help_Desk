package com.divya.helpdesk.service;

import com.divya.helpdesk.dto.ticket.TicketAttachmentResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AttachmentService {

    TicketAttachmentResponseDTO uploadAttachment(Long ticketId, Long messageId, MultipartFile file);

    List<TicketAttachmentResponseDTO> getAttachments(Long ticketId);
}
