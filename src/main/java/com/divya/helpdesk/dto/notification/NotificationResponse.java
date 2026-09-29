package com.divya.helpdesk.dto.notification;

import com.divya.helpdesk.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    private Long id;
    private Long recipientId;
    private String title;
    private String message;
    private NotificationType type;
    private ReferenceTicketDTO reference;
    private Instant createdAt;
}
