package xyz.mobi.notificationservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UnreadNotificationCountResponseDTO(
        long unreadCount
) {
}
