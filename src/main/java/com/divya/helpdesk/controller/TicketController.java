package com.divya.helpdesk.controller;

import com.divya.helpdesk.dto.request.TicketCreateRequest;
import com.divya.helpdesk.dto.request.TicketResolveRequest;
import com.divya.helpdesk.dto.request.TicketUpdateRequest;
import com.divya.helpdesk.dto.response.TicketDetailsResponse;
import com.divya.helpdesk.dto.response.TicketResponse;
import com.divya.helpdesk.enums.HDPriorityLevel;
import com.divya.helpdesk.enums.HDTicketStatus;
import com.divya.helpdesk.service.TicketService;
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
    public ResponseEntity<TicketResponse> createTicket(@Valid @RequestBody TicketCreateRequest request) {
        TicketResponse response = ticketService.createTicket(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<TicketResponse>> getTickets(
            @RequestParam(required = false) Long requesterId,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Long agentId,
            @RequestParam(required = false) HDTicketStatus status,
            @RequestParam(required = false) HDPriorityLevel priority) {

        List<TicketResponse> responses = ticketService.getTickets(requesterId, deptId, agentId, status, priority);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getTicketById(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getTicketById(id));
    }

    @GetMapping("/number/{ticketNumber}")
    public ResponseEntity<TicketResponse> getTicketByNumber(@PathVariable String ticketNumber) {
        return ResponseEntity.ok(ticketService.getTicketByTicketNumber(ticketNumber));
    }

    @GetMapping("/{id}/details")
    public ResponseEntity<TicketDetailsResponse> getTicketDetails(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getTicketDetails(id));
    }

    @PutMapping("/{ticketId}")
    public ResponseEntity<TicketResponse> updateTicket(@PathVariable Long ticketId, @Valid @RequestBody TicketUpdateRequest request) {
        return ResponseEntity.ok(ticketService.updateTicket(ticketId, request));
    }

    @PostMapping("/{id}/start-working")
    public ResponseEntity<TicketResponse> startWorkingOnTicket(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.startWorkingOnTicket(id));
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<TicketResponse> resolveTicket(@PathVariable Long id, @RequestBody(required = false) TicketResolveRequest request) {
        return ResponseEntity.ok(ticketService.resolveTicket(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTicket(@PathVariable Long id) {
        ticketService.deleteTicket(id);
        return ResponseEntity.noContent().build();
    }
}
