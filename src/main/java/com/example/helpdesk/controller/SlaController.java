package com.example.helpdesk.controller;

import com.example.helpdesk.entity.Ticket;
import com.example.helpdesk.entity.TicketSla;
import com.example.helpdesk.repository.TicketRepository;
import com.example.helpdesk.service.SlaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sla")
@RequiredArgsConstructor
@Tag(name = "SLA Management", description = "SLA calculation, monitoring, and lifecycle management")
public class SlaController {

    private final SlaService slaService;
    private final TicketRepository ticketRepository;

    @PostMapping("/create/{ticketId}")
    @Operation(summary = "Create SLA instance", description = "Create an SLA instance for a ticket based on department and sub-category rules")
    public ResponseEntity<TicketSla> createSlaInstance(
            @Parameter(description = "Ticket ID") @PathVariable Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));
        return ResponseEntity.ok(slaService.createSlaInstance(ticket));
    }

    @PutMapping("/update/{ticketId}")
    @Operation(summary = "Update SLA status", description = "Update the SLA status based on current time (RUNNING, WARNING, BREACHED)")
    public ResponseEntity<Void> updateSlaStatus(
            @Parameter(description = "Ticket ID") @PathVariable Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));
        slaService.updateSlaStatus(ticket);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/pause/{ticketId}")
    @Operation(summary = "Pause SLA", description = "Pause the SLA timer for a ticket")
    public ResponseEntity<Void> pauseSla(
            @Parameter(description = "Ticket ID") @PathVariable Long ticketId) {
        slaService.pauseSla(ticketId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/resume/{ticketId}")
    @Operation(summary = "Resume SLA", description = "Resume the SLA timer for a ticket with adjusted deadline")
    public ResponseEntity<Void> resumeSla(
            @Parameter(description = "Ticket ID") @PathVariable Long ticketId) {
        slaService.resumeSla(ticketId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/complete/{ticketId}")
    @Operation(summary = "Complete SLA", description = "Mark the SLA as completed for a ticket")
    public ResponseEntity<Void> completeSla(
            @Parameter(description = "Ticket ID") @PathVariable Long ticketId) {
        slaService.completeSla(ticketId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/check-breaches")
    @Operation(summary = "Check and notify SLA breaches", description = "Check all SLAs for breaches and warnings (scheduled job)")
    public ResponseEntity<Void> checkAndNotifySlaBreaches() {
        slaService.checkAndNotifySlaBreaches();
        return ResponseEntity.ok().build();
    }
}
