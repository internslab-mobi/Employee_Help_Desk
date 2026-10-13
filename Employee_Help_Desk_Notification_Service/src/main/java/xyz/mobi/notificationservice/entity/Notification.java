package xyz.mobi.notificationservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import xyz.mobi.notificationservice.entity.enums.NotificationType;

@Entity
@Table(
        name = "hd_notifications",
        indexes = {
                @Index(
                        name = "idx_notification_recipient",
                        columnList = "recipient_id"
                ),
                @Index(
                        name = "idx_notification_recipient_read_created",
                        columnList = "recipient_id, is_read, created_at"
                ),
                @Index(
                        name = "idx_notification_recipient_created",
                        columnList = "recipient_id, created_at"
                ),
                @Index(
                        name = "idx_notification_ticket",
                        columnList = "ticket_id"
                ),
                @Index(
                        name = "idx_notification_created",
                        columnList = "created_at"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Notification extends BaseEntity {

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Column(name = "ticket_id")
    private Long ticketId;

    @Column(name = "ticket_number", length = 50)
    private String ticketNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "is_read", nullable = false)
    private Boolean read = false;
}
