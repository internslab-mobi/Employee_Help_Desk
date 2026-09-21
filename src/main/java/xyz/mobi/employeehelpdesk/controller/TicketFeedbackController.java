package xyz.mobi.employeehelpdesk.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import xyz.mobi.employeehelpdesk.dto.feedback.TicketFeedbackRequestDto;
import xyz.mobi.employeehelpdesk.dto.feedback.TicketFeedbackResponseDto;
import xyz.mobi.employeehelpdesk.service.TicketFeedbackService;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketFeedbackController {

    private final TicketFeedbackService ticketFeedbackService;

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    @PostMapping("/{ticketId}/feedback")
    public ResponseEntity<TicketFeedbackResponseDto> createFeedback(
            @PathVariable Long ticketId,
            @Valid @RequestBody TicketFeedbackRequestDto request
    ) {
        TicketFeedbackResponseDto response = ticketFeedbackService.createFeedback(ticketId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    @GetMapping("/{ticketId}/feedback")
    public ResponseEntity<TicketFeedbackResponseDto> getFeedback(
            @PathVariable Long ticketId
    ) {
        TicketFeedbackResponseDto response = ticketFeedbackService.getFeedback(ticketId);
        return ResponseEntity.ok(response);
    }
}
