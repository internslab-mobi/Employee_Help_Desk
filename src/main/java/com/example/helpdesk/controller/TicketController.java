package com.example.helpdesk.controller;

import com.example.helpdesk.dto.request.*;
import com.example.helpdesk.dto.response.TicketResponse;
import com.example.helpdesk.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

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
    public ResponseEntity<TicketResponse> createTicket(
            @RequestParam("requesterId") @NotNull Long requesterId,
            @RequestParam("departmentId") @NotNull Long departmentId,
            @RequestParam("categoryId") @NotNull Long categoryId,
            @RequestParam("subCategoryId") @NotNull Long subCategoryId,
            @RequestParam("subject") @NotBlank String subject,
            @RequestParam("description") @NotBlank String description,
            @RequestParam("status") @NotBlank String status,
            @RequestParam(value = "file", required = false) MultipartFile file) {

        CreateTicketRequest request = CreateTicketRequest.builder()
                .requesterId(requesterId)
                .departmentId(departmentId)
                .categoryId(categoryId)
                .subCategoryId(subCategoryId)
                .subject(subject)
                .description(description)
                .status(status)
                .build();

        Long uploadedById = getAuthenticatedEmployeeId();
        TicketResponse response = ticketService.createTicketWithAttachment(request, uploadedById, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    // ==================== CONSOLIDATED TICKET UPDATE ====================

    @PatchMapping("/{ticketId}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    @Operation(description = "🔐 Access: ADMIN, MANAGER, AGENT, EMPLOYEE — Consolidated ticket update endpoint. Authorization checked per operation based on role and ticket ownership/assignment. Use 'operation' field to specify: STATUS, PRIORITY, CATEGORY, ASSIGN_AGENT, ASSIGN_MANAGER, HOLD, RESUME, RESOLVE, REOPEN, WITHDRAW. Each operation has specific required data in the 'data' field.")
    public ResponseEntity<TicketResponse> updateTicket(
            @PathVariable Long ticketId,
            @Valid @RequestBody UpdateDTO request) {
        return ResponseEntity.ok(ticketService.updateTicket(ticketId, request));
    }

}