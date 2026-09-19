package com.example.helpdesk.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    
    private Long id;
    private Long recipientId;
    private Long ticketId;
    private String type;
    private String title;
    private String message;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
