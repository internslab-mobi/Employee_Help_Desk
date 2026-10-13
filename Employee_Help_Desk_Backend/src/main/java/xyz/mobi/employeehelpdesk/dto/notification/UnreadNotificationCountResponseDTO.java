package xyz.mobi.employeehelpdesk.dto.notification;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UnreadNotificationCountResponseDTO(
        long unreadCount
) {
}
