package xyz.mobi.employeehelpdesk.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import xyz.mobi.employeehelpdesk.dto.ticket.HoldTicketRequestDto;
import xyz.mobi.employeehelpdesk.dto.ticket.ResolveTicketRequestDto;
import xyz.mobi.employeehelpdesk.dto.ticket.WithdrawRequestDto;
import xyz.mobi.employeehelpdesk.entity.enums.TicketStatus;
import xyz.mobi.employeehelpdesk.service.CurrentUserService;
import xyz.mobi.employeehelpdesk.service.TicketRoutingService;
import xyz.mobi.employeehelpdesk.dto.ticket.CreateTicketRequest;
import xyz.mobi.employeehelpdesk.dto.ticket.TicketResponse;
import xyz.mobi.employeehelpdesk.service.TicketService;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;
    private final TicketRoutingService ticketRoutingService;
    private final CurrentUserService currentUserService;

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<TicketResponse> createTicket(

            @Valid
            @RequestPart("request")
            CreateTicketRequest request,

            @RequestPart(
                    value = "attachments",
                    required = false
            )
            List<MultipartFile> attachments

    ) throws IOException {

        Long requesterId = currentUserService.getCurrentEmployeeId();

        TicketResponse response =
                ticketService.createTicket(
                        request,
                        attachments,
                        requesterId
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    @PostMapping("/{ticketId}/reopen")
    public ResponseEntity<Void> reopenTicket(
            @PathVariable Long ticketId) {

        Long employeeId = currentUserService.getCurrentEmployeeId();

        ticketRoutingService.reopenTicket(
                ticketId,
                employeeId
        );

        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    @GetMapping("/my/status-summary")
    public ResponseEntity<Map<String, Integer>> getEmployeeTicketStatus() {

        return ResponseEntity.ok(
                ticketService.getEmployeeTicketStatus()
        );
    }

    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @GetMapping("/agent/status-summary")
    public ResponseEntity<Map<String, Integer>> getAgentTicketStatus() {
        return ResponseEntity.ok(
                ticketService.getAgentTicketStatus()
        );
    }

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    @GetMapping("/my")
    public ResponseEntity<Page<TicketResponse>> getMyTickets(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page
    ) {

        Pageable pageable = PageRequest.of(
                page,
                10,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return ResponseEntity.ok(
                ticketService.getMyTickets(
                        status,
                        fromDate,
                        toDate,
                        search,
                        pageable
                )
        );
    }

    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @GetMapping("/agent")
    public ResponseEntity<Page<TicketResponse>> getAgentTickets(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page
    ) {

        Pageable pageable = PageRequest.of(
                page,
                10,
                Sort.by(Sort.Direction.DESC, "assignedAt")
        );

        return ResponseEntity.ok(
                ticketService.getAgentTickets(
                        status,
                        fromDate,
                        toDate,
                        search,
                        pageable
                )
        );
    }

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    @PatchMapping("/withdraw/{ticketId}")
    public ResponseEntity<TicketResponse> withdrawTicket(
            @PathVariable Long ticketId,
            @Valid @RequestBody WithdrawRequestDto withdrawRequest
            ) {
        TicketResponse response =
                ticketService.withdrawTicket(ticketId, withdrawRequest);

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @PostMapping("/{ticketId}/start")
    public ResponseEntity<TicketResponse> startTicket(
            @PathVariable Long ticketId
    ) {
        return ResponseEntity.ok(ticketService.startTicket(ticketId));
    }

    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @PostMapping("/{ticketId}/hold")
    public ResponseEntity<TicketResponse> holdTicket(
            @PathVariable Long ticketId,
            @Valid @RequestBody HoldTicketRequestDto request
    ) {
        return ResponseEntity.ok(ticketService.holdTicket(ticketId, request));
    }

    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @PostMapping("/{ticketId}/resume")
    public ResponseEntity<TicketResponse> resumeTicket(
            @PathVariable Long ticketId
    ) {
        return ResponseEntity.ok(ticketService.resumeTicket(ticketId));
    }

    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @PostMapping("/{ticketId}/resolve")
    public ResponseEntity<TicketResponse> resolveTicket(
            @PathVariable Long ticketId,
            @Valid @RequestBody ResolveTicketRequestDto request
    ) {
        return ResponseEntity.ok(ticketService.resolveTicket(ticketId, request));
    }
}