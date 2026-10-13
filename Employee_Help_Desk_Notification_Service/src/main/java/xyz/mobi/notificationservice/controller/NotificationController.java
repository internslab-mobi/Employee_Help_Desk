package xyz.mobi.notificationservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import xyz.mobi.notificationservice.dto.CreateNotificationRequestDTO;
import xyz.mobi.notificationservice.dto.NotificationResponseDTO;
import xyz.mobi.notificationservice.dto.UnreadNotificationCountResponseDTO;
import xyz.mobi.notificationservice.service.NotificationService;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<NotificationResponseDTO> createNotification(
            @Valid @RequestBody CreateNotificationRequestDTO request
    ) {
        NotificationResponseDTO response = notificationService.createNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<NotificationResponseDTO>> getNotifications(
            @RequestParam(required = false) Long recipientId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<NotificationResponseDTO> response = notificationService.getNotifications(recipientId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/unread")
    public ResponseEntity<Page<NotificationResponseDTO>> getUnreadNotifications(
            @RequestParam(required = false) Long recipientId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<NotificationResponseDTO> response = notificationService.getUnreadNotifications(recipientId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/unread/count")
    public ResponseEntity<UnreadNotificationCountResponseDTO> getUnreadCount(
            @RequestParam(required = false) Long recipientId
    ) {
        UnreadNotificationCountResponseDTO response = notificationService.getUnreadCount(recipientId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponseDTO> markAsRead(
            @PathVariable Long notificationId
    ) {
        NotificationResponseDTO response = notificationService.markAsRead(notificationId);
        return ResponseEntity.ok(response);
    }
}
