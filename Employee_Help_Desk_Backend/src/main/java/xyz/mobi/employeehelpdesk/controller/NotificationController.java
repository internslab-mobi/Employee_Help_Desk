package xyz.mobi.employeehelpdesk.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import xyz.mobi.employeehelpdesk.dto.notification.NotificationResponseDTO;
import xyz.mobi.employeehelpdesk.dto.notification.NotificationUpdateResponseDTO;
import xyz.mobi.employeehelpdesk.dto.notification.UnreadNotificationCountResponseDTO;
import xyz.mobi.employeehelpdesk.service.NotificationService;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    @GetMapping
    public ResponseEntity<Page<NotificationResponseDTO>> getNotifications(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<NotificationResponseDTO> response = notificationService.getNotifications(pageable);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    @GetMapping("/unread")
    public ResponseEntity<Page<NotificationResponseDTO>> getUnreadNotifications(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<NotificationResponseDTO> response = notificationService.getUnreadNotifications(pageable);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    @GetMapping("/unread/count")
    public ResponseEntity<UnreadNotificationCountResponseDTO> getUnreadCount() {
        UnreadNotificationCountResponseDTO response = notificationService.getUnreadCount();
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationUpdateResponseDTO> markAsRead(
            @PathVariable Long notificationId
    ) {
        NotificationUpdateResponseDTO response = notificationService.markAsRead(notificationId);
        return ResponseEntity.ok(response);
    }
}
