package com.example.helpdesk.controller;

import com.example.helpdesk.dto.response.NotificationResponse;
import com.example.helpdesk.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notification management for requesters and agents")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/{employeeId}")
    @Operation(summary = "Get notifications", description = "🔔 Access: All roles — users can view only their own notifications; ADMIN can view notifications for any employee.")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<NotificationResponse>> getNotificationsByEmployee(
            @Parameter(description = "Employee ID") @PathVariable Long employeeId) {
        return ResponseEntity.ok(notificationService.getNotificationsByEmployee(employeeId));
    }

    @PatchMapping("/read/{notificationId}")
    @Operation(summary = "Mark as read", description = "🔔 Access: All roles — users can mark only their own notifications as read; ADMIN can mark any notification.")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    public ResponseEntity<Void> markAsRead(
            @Parameter(description = "Notification ID") @PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/read-all/{employeeId}")
    @Operation(summary = "Mark all as read", description = "🔔 Access: All roles — users can mark only their own notifications as read; ADMIN can mark notifications for any employee.")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    public ResponseEntity<Void> markAllAsReadForEmployee(
            @Parameter(description = "Employee ID") @PathVariable Long employeeId) {
        notificationService.markAllAsReadForEmployee(employeeId);
        return ResponseEntity.ok().build();
    }
}

