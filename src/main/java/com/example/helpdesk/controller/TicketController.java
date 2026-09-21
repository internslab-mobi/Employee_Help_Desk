package com.example.helpdesk.controller;

import com.example.helpdesk.dto.request.AssignTicketRequest;
import com.example.helpdesk.dto.request.CreateTicketRequest;
import com.example.helpdesk.dto.request.HoldTicketRequest;
import com.example.helpdesk.dto.request.ReopenTicketRequest;
import com.example.helpdesk.dto.request.ResolveTicketRequest;
import com.example.helpdesk.dto.request.UpdateTicketCategoryRequest;
import com.example.helpdesk.dto.request.UpdateTicketPriorityRequest;
import com.example.helpdesk.dto.request.UpdateTicketStatusRequest;
import com.example.helpdesk.dto.response.TicketResponse;
import com.example.helpdesk.service.TicketService;
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
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<TicketResponse> createTicket(
            @Valid @RequestBody CreateTicketRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ticketService.createTicket(request));
    }

    @PatchMapping("/{ticketId}/status")
    @PreAuthorize("hasAnyRole('AGENT', 'MANAGER', 'ADMIN')")
    public ResponseEntity<TicketResponse> updateStatus(
            @PathVariable Long ticketId,
            @Valid @RequestBody UpdateTicketStatusRequest request) {

        return ResponseEntity.ok(
                ticketService.updateStatus(ticketId, request)
        );
    }

    @PatchMapping("/{ticketId}/priority")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<TicketResponse> updatePriority(
            @PathVariable Long ticketId,
            @Valid @RequestBody UpdateTicketPriorityRequest request) {

        return ResponseEntity.ok(
                ticketService.updatePriority(ticketId, request)
        );
    }

    @PatchMapping("/{ticketId}/category")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<TicketResponse> updateCategory(
            @PathVariable Long ticketId,
            @Valid @RequestBody UpdateTicketCategoryRequest request) {

        return ResponseEntity.ok(
                ticketService.updateCategory(ticketId, request)
        );
    }


    @PatchMapping("/{ticketId}/assignment")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<TicketResponse> assignTicket(
            @PathVariable Long ticketId,
            @Valid @RequestBody AssignTicketRequest request) {

        return ResponseEntity.ok(
                ticketService.assignTicket(ticketId, request)
        );
    }

    @PatchMapping("/{ticketId}/hold")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<TicketResponse> holdTicket(
            @PathVariable Long ticketId,
            @Valid @RequestBody HoldTicketRequest request) {

        return ResponseEntity.ok(
                ticketService.holdTicket(ticketId, request)
        );
    }

    @PatchMapping("/{ticketId}/resume")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    public ResponseEntity<TicketResponse> resumeTicket(
            @PathVariable Long ticketId) {

        return ResponseEntity.ok(
                ticketService.resumeTicket(ticketId)
        );
    }

    @PatchMapping("/{ticketId}/resolve")
    @PreAuthorize("hasAnyRole('AGENT', 'MANAGER', 'ADMIN')")
    public ResponseEntity<TicketResponse> resolveTicket(
            @PathVariable Long ticketId,
            @Valid @RequestBody ResolveTicketRequest request) {

        return ResponseEntity.ok(
                ticketService.resolveTicketWithSummary(ticketId, request)
        );
    }

    @PatchMapping("/{ticketId}/reopen")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    public ResponseEntity<TicketResponse> reopenTicket(
            @PathVariable Long ticketId) {

        return ResponseEntity.ok(
                ticketService.reopenTicketWithSla(ticketId)
        );
    }
}