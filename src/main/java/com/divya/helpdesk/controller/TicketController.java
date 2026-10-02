package com.divya.helpdesk.controller;

import com.divya.helpdesk.dto.PageResponse;
import com.divya.helpdesk.dto.ticket.*;
import com.divya.helpdesk.enums.TicketStatus;
import com.divya.helpdesk.service.AttachmentService;
import com.divya.helpdesk.service.TicketHistoryService;
import com.divya.helpdesk.service.TicketMessageService;
import com.divya.helpdesk.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;
    private final TicketHistoryService ticketHistoryService;
    private final TicketMessageService ticketMessageService;
    private final AttachmentService attachmentService;

    // Raise ticket (Employee / All authenticated users)
    @PostMapping
    public ResponseEntity<CreateTicketResponseDTO> createTicket(@Valid @RequestBody CreateTicketRequestDTO request){

        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.createTicket(request));
    }

    // Single flexible PATCH endpoint for tickets
    @PatchMapping("/{id}")
    public ResponseEntity<CreateTicketResponseDTO> patchTicket(@PathVariable Long id, @Valid @RequestBody TicketPatchRequestDTO request){

        return ResponseEntity.ok(ticketService.patchTicket(id, request));
    }

    // Edit ticket via PUT (Employee while NEW, or Manager/Admin)
    @PutMapping("/{id}")
    public ResponseEntity<CreateTicketResponseDTO> updateTicket(@PathVariable Long id, @Valid @RequestBody UpdateTicketRequestDTO request){

        return ResponseEntity.ok(ticketService.updateTicket(id, request));
    }

    // Delete ticket (Department Manager / Admin)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Void> deleteTicket(@PathVariable Long id) {
        ticketService.deleteTicket(id);

        return ResponseEntity.noContent().build();
    }

    // View own tickets (Employee)
    @GetMapping("/me")
    public ResponseEntity<List<CreateTicketResponseDTO>> getMyTickets(@RequestParam(required = false) TicketStatus status){

        return ResponseEntity.ok(ticketService.getMyTickets(status));
    }

    // View assigned tickets (Agent / Manager)
    @GetMapping("/assigned")
    @PreAuthorize("hasAnyRole('AGENT', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<CreateTicketResponseDTO>> getAssignedTickets(@RequestParam(required = false) TicketStatus status){

        return ResponseEntity.ok(ticketService.getAssignedTickets(status));
    }

    // View department tickets (Manager / Admin)
    @GetMapping("/department")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<PageResponse<CreateTicketResponseDTO>> getDepartmentTickets(
            @RequestParam(required = false) Long agentId,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") long offset){

        return ResponseEntity.ok(ticketService.getDepartmentTickets(agentId, status, limit, offset));
    }

    // Withdraw ticket (Employee)
    @PutMapping("/{id}/withdraw")
    public ResponseEntity<Void> withdrawTicket(@PathVariable Long id, @Valid @RequestBody WithdrawTicketRequestDTO request){
        ticketService.withdrawTicket(id, request);

        return ResponseEntity.noContent().build();
    }

    // Start working on ticket (Assigned Agent / Manager / Admin)
    @PostMapping("/{id}/start-working")
    @PreAuthorize("hasAnyRole('AGENT', 'MANAGER', 'ADMIN')")
    public ResponseEntity<CreateTicketResponseDTO> startWorking(@PathVariable Long id) {

        return ResponseEntity.ok(ticketService.startWorking(id));
    }

    // Resolve ticket (Agent / Manager / Admin)
    @PutMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('AGENT', 'MANAGER', 'ADMIN')")
    public ResponseEntity<CreateTicketResponseDTO> resolveTicket(@PathVariable Long id, @Valid @RequestBody ResolveTicketRequestDTO request){

        return ResponseEntity.ok(ticketService.resolveTicket(id, request));
    }

    // Set waiting for employee (Agent / Manager)
    @PutMapping("/{id}/waiting")
    @PreAuthorize("hasAnyRole('AGENT', 'MANAGER', 'ADMIN')")
    public ResponseEntity<CreateTicketResponseDTO> waiting(@PathVariable Long id, @Valid @RequestBody WaitingForEmployeeRequestDTO request){

        return ResponseEntity.ok(ticketService.waitingForEmployee(id, request));
    }

    // Resume ticket (Agent / Manager)
    @PutMapping("/{id}/resume")
    @PreAuthorize("hasAnyRole('AGENT', 'MANAGER', 'ADMIN')")
    public ResponseEntity<CreateTicketResponseDTO> resume(@PathVariable Long id) {

        return ResponseEntity.ok(ticketService.resumeTicket(id));
    }

    // Reopen ticket (Employee)
    @PutMapping("/{id}/reopen")
    public ResponseEntity<CreateTicketResponseDTO> reopen(@PathVariable Long id, @Valid @RequestBody ReopenTicketRequestDTO request){

        return ResponseEntity.ok(ticketService.reopenTicket(id, request));
    }

    // Submit feedback (Employee)
    @PostMapping("/{id}/feedback")
    public ResponseEntity<Void> submitFeedback(@PathVariable Long id, @Valid @RequestBody FeedbackRequestDTO request){
        ticketService.submitFeedback(id, request);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // Send ticket message (Requester / Agent / Manager)
    @PostMapping("/{ticketId}/messages")
    public ResponseEntity<TicketMessageResponseDTO> sendMessage(@PathVariable Long ticketId, @Valid @RequestBody SendMessageRequestDTO request){

        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.sendMessage(ticketId, request));
    }

    // View ticket messages
    @GetMapping("/{id}/messages")
    public ResponseEntity<List<TicketMessageResponseDTO>> getMessages(@PathVariable Long id) {

        return ResponseEntity.ok(ticketMessageService.getMessages(id));
    }

    // View ticket history
    @GetMapping("/{id}/history")
    public ResponseEntity<List<TicketHistoryResponseDTO>> getHistory(@PathVariable Long id) {

        return ResponseEntity.ok(ticketHistoryService.getHistory(id));
    }

    // ATTACHMENT ENDPOINTS (BLOB Storage)

    // Upload attachment (BLOB)
    @PostMapping(value = "/{ticketId}/message/{messageId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TicketAttachmentResponseDTO> uploadAttachment(@PathVariable Long ticketId, @PathVariable Long messageId, @RequestParam("file") MultipartFile file){
        TicketAttachmentResponseDTO response = attachmentService.uploadAttachment(ticketId, messageId, file);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // View attachment metadata list (No BLOB content exposed)
    @GetMapping("/{id}/attachments")
    public ResponseEntity<List<TicketAttachmentResponseDTO>> getAttachments(@PathVariable Long id){

        return ResponseEntity.ok(attachmentService.getAttachments(id));
    }

}