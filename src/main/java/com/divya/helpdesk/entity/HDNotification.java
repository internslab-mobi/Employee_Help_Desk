package com.divya.helpdesk.entity;

import com.divya.helpdesk.enums.HDNotificationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "hd_notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HDNotification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private HDEmployee recipient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id")
    private HDTicket ticket;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private HDNotificationType type;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "read_at")
    private LocalDateTime readAt;
}
