package com.example.helpdesk.service;

import com.example.helpdesk.dto.response.TicketAttachmentResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface TicketAttachmentService {

    TicketAttachmentResponse uploadAttachment(Long ticketId, Long uploadedById, MultipartFile file, Long messageId, String attachmentType);

    List<TicketAttachmentResponse> getTicketAttachments(Long ticketId);

    Resource downloadAttachment(Long attachmentId);

    void deleteAttachment(Long attachmentId, Long requesterId);

    TicketAttachmentResponse uploadMessageAttachment(Long ticketId, Long messageId, Long uploadedById, MultipartFile file);
}
