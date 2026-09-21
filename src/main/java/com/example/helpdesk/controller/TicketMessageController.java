package com.example.helpdesk.controller;

import com.example.helpdesk.dto.request.TicketMessageRequest;
import com.example.helpdesk.dto.response.TicketMessageResponse;
import com.example.helpdesk.service.TicketMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketMessageController {

    private final TicketMessageService ticketMessageService;

    @PostMapping("/{ticketId}/messages")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    public ResponseEntity<TicketMessageResponse> sendMessage(
            @PathVariable Long ticketId,
            @Valid @RequestBody TicketMessageRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ticketMessageService.sendMessage(ticketId, request));
    }

    @GetMapping("/{ticketId}/messages")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<TicketMessageResponse>> getTicketMessages(
            @PathVariable Long ticketId) {

        return ResponseEntity.ok(ticketMessageService.getTicketMessages(ticketId));
    }

    @PatchMapping("/ticket-messages/{messageId}/seen")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    public ResponseEntity<Void> markAsSeen(
            @PathVariable Long messageId) {

        ticketMessageService.markAsSeen(messageId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{ticketId}/messages/unread")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<TicketMessageResponse>> getUnreadMessages(
            @PathVariable Long ticketId,
            @RequestParam Long recipientId) {

        return ResponseEntity.ok(ticketMessageService.getUnreadMessages(ticketId, recipientId));
    }
}
