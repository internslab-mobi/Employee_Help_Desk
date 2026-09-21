package com.divya.helpdesk.controller;

import com.divya.helpdesk.dto.request.TicketCreateRequest;
import com.divya.helpdesk.dto.request.TicketResolveRequest;
import com.divya.helpdesk.dto.request.TicketUpdateRequest;
import com.divya.helpdesk.dto.response.TicketDetailsResponse;
import com.divya.helpdesk.dto.response.TicketResponse;
import com.divya.helpdesk.enums.HDPriorityLevel;
import com.divya.helpdesk.enums.HDTicketStatus;
import com.divya.helpdesk.security.CurrentUserService;
import com.divya.helpdesk.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;
    private final CurrentUserService currentUserService;

    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(@Valid @RequestBody TicketCreateRequest request) {
        Long employeeId = currentUserService.getEmployeeId();
        return ResponseEntity.ok(ticketService.createTicket(request, employeeId));
    }

    @GetMapping
    public ResponseEntity<List<TicketResponse>> getTickets(@RequestParam(required = false) Long deptId, @RequestParam(required = false) Long agentId,
            @RequestParam(required = false) HDTicketStatus status, @RequestParam(required = false) HDPriorityLevel priority) {
        Long employeeId = currentUserService.getEmployeeId();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String role = authentication.getAuthorities()
                .iterator()
                .next()
                .getAuthority();
        return ResponseEntity.ok(ticketService.getTickets(employeeId, role, deptId, agentId, status, priority));
    }

    @GetMapping("/{ticketId}")
    public ResponseEntity<TicketResponse> getTicketById(@PathVariable Long ticketId) {
        Long employeeId = currentUserService.getEmployeeId();
        return ResponseEntity.ok(ticketService.getTicketById(ticketId, employeeId));
    }

    @GetMapping("/number/{ticketNumber}")
    public ResponseEntity<TicketResponse> getTicketByNumber(@PathVariable String ticketNumber) {
        Long employeeId = currentUserService.getEmployeeId();
        return ResponseEntity.ok(ticketService.getTicketByTicketNumber(ticketNumber, employeeId));
    }

    @GetMapping("/{ticketId}/details")
    public ResponseEntity<TicketDetailsResponse> getTicketDetails(@PathVariable Long ticketId) {
        return ResponseEntity.ok(ticketService.getTicketDetails(ticketId, currentUserService.getEmployeeId()));
    }

    @PutMapping("/{ticketId}")
    public ResponseEntity<TicketResponse> updateTicket(@PathVariable Long ticketId, @Valid @RequestBody TicketUpdateRequest request) {
        Long employeeId = currentUserService.getEmployeeId();
        return ResponseEntity.ok(ticketService.updateTicket(ticketId, request, employeeId));
    }

    @PostMapping("/{ticketId}/start-working")
    public ResponseEntity<TicketResponse> startWorkingOnTicket(@PathVariable Long ticketId) {
        Long employeeId = currentUserService.getEmployeeId();
        return ResponseEntity.ok(ticketService.startWorkingOnTicket(ticketId, employeeId));
    }

    @PostMapping("/{ticketId}/resolve")
    public ResponseEntity<TicketResponse> resolveTicket(@PathVariable Long ticketId, @RequestBody(required = false) TicketResolveRequest request) {
        Long employeeId = currentUserService.getEmployeeId();
        return ResponseEntity.ok(ticketService.resolveTicket(ticketId, request, employeeId));
    }

    @DeleteMapping("/{ticketId}")
    public ResponseEntity<Void> deleteTicket(@PathVariable Long ticketId) {
        Long employeeId = currentUserService.getEmployeeId();
        ticketService.deleteTicket(ticketId, employeeId);
        return ResponseEntity.noContent().build();
    }
}
