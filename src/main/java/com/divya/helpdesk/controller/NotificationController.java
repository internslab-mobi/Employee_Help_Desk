package com.divya.helpdesk.controller;

import com.divya.helpdesk.dto.notification.NotificationResponse;
import com.divya.helpdesk.security.CurrentUserService;
import com.divya.helpdesk.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUserService currentUserService;

    @GetMapping("/me")
    public ResponseEntity<List<NotificationResponse>> getMyNotifications() {
        Long employeeId = currentUserService.getEmployeeId();
        return ResponseEntity.ok(notificationService.getMyNotifications(employeeId));
    }
}
