package xyz.mobi.employeehelpdesk.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import xyz.mobi.employeehelpdesk.dto.feedback.TicketFeedbackCreateResponse;
import xyz.mobi.employeehelpdesk.dto.feedback.TicketFeedbackRequestDto;
import xyz.mobi.employeehelpdesk.dto.feedback.TicketFeedbackResponseDto;
import xyz.mobi.employeehelpdesk.dto.ticket.*;
import xyz.mobi.employeehelpdesk.entity.enums.SlaStatus;
import xyz.mobi.employeehelpdesk.entity.enums.TicketStatus;
import xyz.mobi.employeehelpdesk.entity.enums.TicketView;
import xyz.mobi.employeehelpdesk.service.AuthService;
import xyz.mobi.employeehelpdesk.service.TicketService;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;
    private final AuthService authService;

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TicketCreateResponse> createTicket(
            @Valid @RequestPart("request") CreateTicketRequest request,
            @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments
    ) throws IOException {

        Long requesterId = authService.getCurrentEmployeeId();

        TicketCreateResponse response = ticketService.createTicket(
                request,
                attachments,
                requesterId
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    @PatchMapping("/{ticketId}")
    public ResponseEntity<TicketUpdateResponse> updateTicket(
            @PathVariable Long ticketId,
            @Valid @RequestBody UpdateTicketRequest request
    ) {

        return ResponseEntity.ok(
                ticketService.updateTicket(
                        ticketId,
                        request
                )
        );
    }

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    @GetMapping("/{ticketId}")
    public ResponseEntity<TicketResponse> getTicket(
            @PathVariable Long ticketId
    ) {

        return ResponseEntity.ok(
                ticketService.getTicket(ticketId)
        );
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @GetMapping("/{ticketId}/assignable-agents")
    public ResponseEntity<List<AssignableAgentResponse>> getAssignableAgents(
            @PathVariable Long ticketId
    ) {

        return ResponseEntity.ok(
                ticketService.getAssignableAgents(ticketId)
        );
    }

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    @GetMapping
    public ResponseEntity<Page<TicketResponse>> getAllTickets(
            @RequestParam TicketView view,
            @RequestParam(required = false) Long employeeId,
            @PageableDefault(
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {

        return ResponseEntity.ok(
                ticketService.getAllTickets(
                        view,
                        employeeId,
                        pageable
                )
        );
    }

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    @GetMapping("/search")
    public ResponseEntity<Page<TicketResponse>> searchTickets(
            @RequestParam TicketView view,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean unassigned,
            @RequestParam(required = false) SlaStatus slaStatus,
            @PageableDefault(
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {

        return ResponseEntity.ok(
                ticketService.searchTickets(
                        view,
                        employeeId,
                        status,
                        fromDate,
                        toDate,
                        search,
                        unassigned,
                        slaStatus,
                        pageable
                )
        );
    }

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Integer>> getTicketSummary(
            @RequestParam TicketView view,
            @RequestParam(required = false) Long employeeId
    ) {

        return ResponseEntity.ok(
                ticketService.getTicketSummary(
                        view,
                        employeeId
                )
        );
    }

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    @PostMapping("/{ticketId}/feedback")
    public ResponseEntity<TicketFeedbackCreateResponse> createFeedback(
            @PathVariable Long ticketId,
            @Valid @RequestBody TicketFeedbackRequestDto request
    ) {

        TicketFeedbackCreateResponse response =
                ticketService.createFeedback(
                        ticketId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    @GetMapping("/{ticketId}/feedback")
    public ResponseEntity<TicketFeedbackResponseDto> getFeedback(
            @PathVariable Long ticketId
    ) {

        TicketFeedbackResponseDto response =
                ticketService.getFeedback(ticketId);

        return ResponseEntity.ok(response);
    }
}