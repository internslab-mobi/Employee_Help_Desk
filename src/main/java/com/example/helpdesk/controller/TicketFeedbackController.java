package com.example.helpdesk.controller;

import com.example.helpdesk.dto.request.TicketFeedbackRequest;
import com.example.helpdesk.dto.response.TicketFeedbackResponse;
import com.example.helpdesk.service.TicketFeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketFeedbackController {

    private final TicketFeedbackService ticketFeedbackService;

    @PostMapping("/{ticketId}/feedback")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    public ResponseEntity<TicketFeedbackResponse> submitFeedback(
            @PathVariable Long ticketId,
            @Valid @RequestBody TicketFeedbackRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ticketFeedbackService.submitFeedback(ticketId, request));
    }

    @GetMapping("/{ticketId}/feedback")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    public ResponseEntity<TicketFeedbackResponse> getTicketFeedback(
            @PathVariable Long ticketId) {

        Optional<TicketFeedbackResponse> feedback = ticketFeedbackService.getTicketFeedback(ticketId);
        return feedback.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
