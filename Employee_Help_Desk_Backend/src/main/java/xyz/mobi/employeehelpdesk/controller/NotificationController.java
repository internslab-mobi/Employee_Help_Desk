package xyz.mobi.employeehelpdesk.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import xyz.mobi.employeehelpdesk.dto.notification.NotificationResponseDTO;
import xyz.mobi.employeehelpdesk.dto.notification.UnreadNotificationCountResponseDTO;
import xyz.mobi.employeehelpdesk.security.UserPrincipal;
import xyz.mobi.employeehelpdesk.service.NotificationService;

@RestController
@RequestMapping("/helpdesk/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    @GetMapping
    public ResponseEntity<Page<NotificationResponseDTO>> getNotifications(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<NotificationResponseDTO> response = notificationService.getNotifications(pageable, userPrincipal);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    @GetMapping("/unread")
    public ResponseEntity<Page<NotificationResponseDTO>> getUnreadNotifications(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<NotificationResponseDTO> response = notificationService.getUnreadNotifications(pageable, userPrincipal);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    @GetMapping("/unread/count")
    public ResponseEntity<UnreadNotificationCountResponseDTO> getUnreadCount(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        UnreadNotificationCountResponseDTO response = notificationService.getUnreadCount(userPrincipal);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponseDTO> markAsRead(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long notificationId
    ) {
        NotificationResponseDTO response = notificationService.markAsRead(notificationId, userPrincipal);
        return ResponseEntity.ok(response);
    }
}
