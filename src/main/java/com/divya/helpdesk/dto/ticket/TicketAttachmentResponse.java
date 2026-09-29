package com.divya.helpdesk.dto.ticket;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.OffsetDateTime;

/**
 * Metadata DTO for Ticket Attachments.
 * Excludes the binary BLOB data to keep GET/list responses lightweight.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TicketAttachmentResponse {

    private Long id;
    private Long ticketId;
    private String originalFilename;
    private String mimeType;
    private Long fileSize;
    private OffsetDateTime createdAt;
}
