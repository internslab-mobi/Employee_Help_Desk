package com.divya.helpdesk.service;

import com.divya.helpdesk.dto.ticket.TicketAttachmentResponse;
import com.divya.helpdesk.entity.HDTicketAttachment;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AttachmentService {

    TicketAttachmentResponse uploadAttachment(Long ticketId, Long messageId, MultipartFile file);

    List<TicketAttachmentResponse> getAttachments(Long ticketId);
}
