package com.example.helpdesk.controller;

import com.example.helpdesk.dto.request.AssignTicketRequest;
import com.example.helpdesk.dto.request.CreateTicketRequest;
import com.example.helpdesk.dto.request.ReopenTicketRequest;
import com.example.helpdesk.dto.request.UpdateTicketCategoryRequest;
import com.example.helpdesk.dto.request.UpdateTicketPriorityRequest;
import com.example.helpdesk.dto.request.UpdateTicketStatusRequest;
import com.example.helpdesk.dto.response.TicketResponse;
import com.example.helpdesk.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(
            @Valid @RequestBody CreateTicketRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ticketService.createTicket(request));
    }

    @PatchMapping("/{ticketId}/status")
    public ResponseEntity<TicketResponse> updateStatus(
            @PathVariable Long ticketId,
            @Valid @RequestBody UpdateTicketStatusRequest request) {

        return ResponseEntity.ok(
                ticketService.updateStatus(ticketId, request)
        );
    }

    @PatchMapping("/{ticketId}/priority")
    public ResponseEntity<TicketResponse> updatePriority(
            @PathVariable Long ticketId,
            @Valid @RequestBody UpdateTicketPriorityRequest request) {

        return ResponseEntity.ok(
                ticketService.updatePriority(ticketId, request)
        );
    }

    @PatchMapping("/{ticketId}/category")
    public ResponseEntity<TicketResponse> updateCategory(
            @PathVariable Long ticketId,
            @Valid @RequestBody UpdateTicketCategoryRequest request) {

        return ResponseEntity.ok(
                ticketService.updateCategory(ticketId, request)
        );
    }


    @PatchMapping("/{ticketId}/assignment")
    public ResponseEntity<TicketResponse> assignTicket(
            @PathVariable Long ticketId,
            @Valid @RequestBody AssignTicketRequest request) {

        return ResponseEntity.ok(
                ticketService.assignTicket(ticketId, request)
        );
    }


    @PatchMapping("/{ticketId}/resolve")
    public ResponseEntity<TicketResponse> resolveTicket(
            @PathVariable Long ticketId,
            @Valid @RequestBody UpdateTicketStatusRequest request) {

        return ResponseEntity.ok(
                ticketService.resolveTicket(ticketId, request)
        );
    }


    @PatchMapping("/{ticketId}/reopen")
    public ResponseEntity<TicketResponse> reopenTicket(
            @PathVariable Long ticketId,
            @Valid @RequestBody ReopenTicketRequest request) {

        return ResponseEntity.ok(
                ticketService.reopenTicket(ticketId, request)
        );
    }
}