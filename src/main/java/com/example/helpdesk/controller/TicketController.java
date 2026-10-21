package com.example.helpdesk.controller;

<<<<<<< HEAD
import com.example.helpdesk.dto.request.AssignTicketRequest;
import com.example.helpdesk.dto.request.CreateTicketRequest;
import com.example.helpdesk.dto.request.ReopenTicketRequest;
import com.example.helpdesk.dto.request.UpdateTicketCategoryRequest;
import com.example.helpdesk.dto.request.UpdateTicketPriorityRequest;
import com.example.helpdesk.dto.request.UpdateTicketStatusRequest;
import com.example.helpdesk.dto.response.TicketResponse;
=======
import com.example.helpdesk.dto.request.*;
import com.example.helpdesk.dto.response.TicketResponseDTO;
>>>>>>> c5b2367e (updated reworks)
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

<<<<<<< HEAD
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
=======
    private Long getAuthenticatedEmployeeId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new com.example.helpdesk.exception.AuthorizationException("User not authenticated");
        }
        try {
            return Long.parseLong(authentication.getPrincipal().toString());
        } catch (NumberFormatException e) {
            throw new com.example.helpdesk.exception.AuthorizationException("Invalid employee ID in authentication context");
        }
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN', 'MANAGER', 'AGENT')")
    @Operation(description = "🔐 Access: ADMIN, MANAGER, AGENT, EMPLOYEE — authenticated users can create tickets; agent, manager and SLA are handled automatically. Optional attachment support.")
    public ResponseEntity<TicketResponseDTO> createTicket(
            @RequestParam("requesterId") @NotNull Long requesterId,
            @RequestParam("departmentId") @NotNull Long departmentId,
            @RequestParam("categoryId") @NotNull Long categoryId,
            @RequestParam("subCategoryId") @NotNull Long subCategoryId,
            @RequestParam("subject") @NotBlank String subject,
            @RequestParam("description") @NotBlank String description,
            @RequestParam("status") @NotBlank String status,
            @RequestParam(value = "file", required = false) MultipartFile file) {

        CreateTicketRequestDTO request = CreateTicketRequestDTO.builder()
                .requesterId(requesterId)
                .departmentId(departmentId)
                .categoryId(categoryId)
                .subCategoryId(subCategoryId)
                .subject(subject)
                .description(description)
                .status(status)
                .build();

        Long uploadedById = getAuthenticatedEmployeeId();
        TicketResponseDTO response = ticketService.createTicketWithAttachment(request, uploadedById, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    // ==================== CONSOLIDATED TICKET UPDATE ====================

    @PatchMapping("/{ticketId}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    @Operation(description = "🔐 Access: ADMIN, MANAGER, AGENT, EMPLOYEE — Consolidated ticket update endpoint. Authorization checked per operation based on role and ticket ownership/assignment. Use 'operation' field to specify: STATUS, PRIORITY, CATEGORY, ASSIGN_AGENT, ASSIGN_MANAGER, HOLD, RESUME, RESOLVE, REOPEN, WITHDRAW. Each operation has specific required data in the 'data' field.")
    public ResponseEntity<TicketResponseDTO> updateTicket(
            @PathVariable Long ticketId,
            @Valid @RequestBody UpdateTicketRequestDTO request) {
        return ResponseEntity.ok(ticketService.updateTicket(ticketId, request));
    }

}



>>>>>>> c5b2367e (updated reworks)
