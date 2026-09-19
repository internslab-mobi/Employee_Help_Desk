package com.example.helpdesk.controller;

import com.example.helpdesk.entity.Employee;
import com.example.helpdesk.entity.Ticket;
import com.example.helpdesk.enums.NotificationType;
import com.example.helpdesk.repository.EmployeeRepository;
import com.example.helpdesk.repository.TicketRepository;
import com.example.helpdesk.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notification management for requesters and agents")
public class NotificationController {

    private final NotificationService notificationService;
    private final EmployeeRepository employeeRepository;
    private final TicketRepository ticketRepository;

    @PostMapping("/send")
    @Operation(summary = "Send notification", description = "Send a notification to an employee about a ticket")
    public ResponseEntity<Void> sendNotification(
            @Parameter(description = "Employee ID") @RequestParam Long employeeId,
            @Parameter(description = "Ticket ID") @RequestParam Long ticketId,
            @Parameter(description = "Notification type") @RequestParam NotificationType type,
            @Parameter(description = "Notification title") @RequestParam String title,
            @Parameter(description = "Notification message") @RequestParam String message) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));
        notificationService.sendNotification(employee, ticket, type, title, message);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/read/{notificationId}")
    @Operation(summary = "Mark as read", description = "Mark a notification as read")
    public ResponseEntity<Void> markAsRead(
            @Parameter(description = "Notification ID") @PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/read-all/{employeeId}")
    @Operation(summary = "Mark all as read", description = "Mark all notifications for an employee as read")
    public ResponseEntity<Void> markAllAsReadForEmployee(
            @Parameter(description = "Employee ID") @PathVariable Long employeeId) {
        notificationService.markAllAsReadForEmployee(employeeId);
        return ResponseEntity.ok().build();
    }
}
