package com.example.helpdesk.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketAttachmentResponse {

    private Long id;
    private Long ticketId;
    private Long uploadedById;
    private String uploadedByName;
    private Long messageId;
    private String originalFilename;
    private String mimeType;
    private Long fileSize;
    private String attachmentType;
    private OffsetDateTime createdAt;
}
