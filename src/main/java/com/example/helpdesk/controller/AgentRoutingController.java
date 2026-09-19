package com.example.helpdesk.controller;

import com.example.helpdesk.dto.response.AssignmentProposalResponse;
import com.example.helpdesk.service.AgentRoutingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/routing")
@RequiredArgsConstructor
@Tag(name = "Agent Routing", description = "Intelligent agent routing and assignment confirmation")
public class AgentRoutingController {

    private final AgentRoutingService agentRoutingService;

    @GetMapping("/proposal/{ticketId}")
    @Operation(summary = "Get assignment proposal", description = "Get the best agent assignment proposal for a ticket based on skills, workload, and availability")
    public ResponseEntity<AssignmentProposalResponse> getAssignmentProposal(
            @Parameter(description = "Ticket ID") @PathVariable Long ticketId) {
        return ResponseEntity.ok(agentRoutingService.getAssignmentProposal(ticketId));
    }

    @PostMapping("/confirm")
    @Operation(summary = "Confirm assignment", description = "Confirm or reject an agent assignment for a ticket")
    public ResponseEntity<Void> confirmAssignment(
            @Parameter(description = "Ticket ID") @RequestParam Long ticketId,
            @Parameter(description = "Agent ID") @RequestParam Long agentId,
            @Parameter(description = "Confirmation status") @RequestParam Boolean confirmed) {
        agentRoutingService.confirmAssignment(ticketId, agentId, confirmed);
        return ResponseEntity.ok().build();
    }
}
