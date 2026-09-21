package xyz.mobi.employeehelpdesk.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import xyz.mobi.employeehelpdesk.entity.enums.AttachmentType;
import xyz.mobi.employeehelpdesk.exception.BadRequestException;

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
    @JoinColumn(name = "message_id", nullable = true)
    private TicketMessage message;

    @Enumerated(EnumType.STRING)
    @Column(name = "attachment_type", nullable = false, length = 30)
    private AttachmentType attachmentType;

    /**
     * Validates attachment type relationships before persist/update.
     * <p>
     * INITIAL_ATTACHMENT: ticket required, message null, employee null
     * TICKET_MESSAGE: ticket required, message required, employee null
     * PROFILE_IMG: ticket null, message null, employee required
     */
    @PrePersist
    @PreUpdate
    private void validateAttachmentCombination() {
        if (attachmentType == null) {
            return;
        }
        switch (attachmentType) {
            case INITIAL_ATTACHMENT -> {
                if (ticket == null) {
                    throw new BadRequestException("INITIAL_ATTACHMENT requires a ticket");
                }
            }
            case TICKET_MESSAGE -> {
                if (ticket == null || message == null) {
                    throw new BadRequestException("TICKET_MESSAGE requires both a ticket and a message");
                }
            }
            case PROFILE_IMG -> {
                if (employee == null) {
                    throw new BadRequestException("PROFILE_IMG requires an employee");
                }
                if (ticket != null) {
                    throw new BadRequestException("PROFILE_IMG must not be associated with a ticket");
                }
            }
        }
    }
}