package xyz.mobi.employeehelpdesk.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import xyz.mobi.employeehelpdesk.entity.enums.AttachmentType;

@Entity
@Table(
        name = "ticket_attachments",
        indexes = {
                @Index(
                        name = "idx_ticket_attachment_ticket",
                        columnList = "ticket_id"
                ),
                @Index(
                        name = "idx_ticket_attachment_uploaded_by",
                        columnList = "uploaded_by"
                ),
                @Index(
                        name = "idx_message_attachment_message",
                        columnList = "message_id"
                ),
                @Index(
                        name = "idx_attachment_employee",
                        columnList = "employee_id"
                ),
                @Index(
                        name = "idx_attachment_type",
                        columnList = "attachment_type"
                )
        }
)
@Getter
@Setter
public class TicketAttachment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id")
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private Employee uploadedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Column(nullable = false, length = 255)
    private String originalFilename;

    @Column(length = 100)
    private String mimeType;

    private Long fileSize;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGBLOB")
    private byte[] fileData;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id")
    private TicketMessage message;

    @Enumerated(EnumType.STRING)
    @Column(name = "attachment_type", nullable = false, length = 30)
    private AttachmentType attachmentType;

}