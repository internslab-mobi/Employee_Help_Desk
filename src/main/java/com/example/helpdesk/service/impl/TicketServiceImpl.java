package com.example.helpdesk.service.impl;

import com.example.helpdesk.dto.request.AssignTicketRequest;
import com.example.helpdesk.dto.request.CreateTicketRequest;
import com.example.helpdesk.dto.request.HoldTicketRequest;
import com.example.helpdesk.dto.request.ReopenTicketRequest;
import com.example.helpdesk.dto.request.ResolveTicketRequest;
import com.example.helpdesk.dto.request.UpdateTicketCategoryRequest;
import com.example.helpdesk.dto.request.UpdateTicketPriorityRequest;
import com.example.helpdesk.dto.request.UpdateTicketStatusRequest;
import com.example.helpdesk.dto.response.TicketResponse;
import com.example.helpdesk.entity.Category;
import com.example.helpdesk.entity.Department;
import com.example.helpdesk.entity.DepartmentAgent;
import com.example.helpdesk.entity.Employee;
import com.example.helpdesk.entity.SubCategory;
import com.example.helpdesk.entity.Ticket;
import com.example.helpdesk.entity.TicketSla;
import com.example.helpdesk.enums.NotificationType;
import com.example.helpdesk.enums.SlaStatus;
import com.example.helpdesk.enums.TicketEventType;
import com.example.helpdesk.enums.TicketStatus;
import com.example.helpdesk.mapper.TicketMapper;
import com.example.helpdesk.repository.CategoryRepository;
import com.example.helpdesk.repository.DepartmentAgentRepository;
import com.example.helpdesk.repository.DepartmentRepository;
import com.example.helpdesk.repository.EmployeeRepository;
import com.example.helpdesk.repository.SubCategoryRepository;
import com.example.helpdesk.repository.TicketRepository;
import com.example.helpdesk.repository.TicketSlaRepository;
import com.example.helpdesk.service.NotificationService;
import com.example.helpdesk.service.SlaService;
import com.example.helpdesk.service.TicketHistoryService;
import com.example.helpdesk.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final TicketMapper ticketMapper;

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;

    private final DepartmentAgentRepository departmentAgentRepository;
    private final TicketSlaRepository ticketSlaRepository;

    private final SlaService slaService;
    private final TicketHistoryService ticketHistoryService;
    private final NotificationService notificationService;

    private final AtomicInteger ticketSequence = new AtomicInteger(1);

    private String generateTicketNumber() {
        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int sequence = ticketSequence.getAndIncrement();
        return "TKT-" + timestamp + "-" + String.format("%04d", sequence);
    }

    @Override
    public TicketResponse createTicket(CreateTicketRequest request) {

        // ---------------------------------------------------------
        // 1. Find requester
        // ---------------------------------------------------------
        Employee requester = employeeRepository.findById(request.getRequesterId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Employee not found: " + request.getRequesterId()
                        )
                );


        // ---------------------------------------------------------
        // 2. Find department
        // ---------------------------------------------------------
        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Department not found: " + request.getDepartmentId()
                        )
                );


        // ---------------------------------------------------------
        // 3. Find category
        // Category is mandatory
        // ---------------------------------------------------------
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Category not found: " + request.getCategoryId()
                        )
                );


        // ---------------------------------------------------------
        // 4. Find sub-category
        // Sub-category is mandatory
        // ---------------------------------------------------------
        SubCategory subCategory =
                subCategoryRepository.findById(request.getSubCategoryId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "SubCategory not found: "
                                                + request.getSubCategoryId()
                                )
                        );


        // ---------------------------------------------------------
        // 5. Get priority from sub-category
        // ---------------------------------------------------------
        String priority = subCategory.getPriority();


        // ---------------------------------------------------------
        // 6. Generate ticket number
        // ---------------------------------------------------------
        String ticketNumber = generateTicketNumber();


        // ---------------------------------------------------------
        // 7. Create ticket
        // ---------------------------------------------------------
        Ticket ticket = ticketMapper.toEntity(
                request,
                requester,
                department,
                category,
                subCategory,
                ticketNumber,
                priority
        );


        // ---------------------------------------------------------
        // 8. Save ticket
        // ---------------------------------------------------------
        Ticket savedTicket = ticketRepository.save(ticket);


        log.info(
                "Ticket created. ticketId={}, ticketNumber={}",
                savedTicket.getId(),
                savedTicket.getTicketNumber()
        );

        return ticketMapper.toResponse(savedTicket);
    }



    @Override
    public TicketResponse updateStatus(Long ticketId, UpdateTicketStatusRequest request) {

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Ticket not found: " + ticketId));

        String newStatus = request.getStatus();

        ticket.setStatus(newStatus);

        // Employee communication required -> pause SLA
        if (TicketStatus.NEED_EMPLOYEE_COMMUNICATION.name().equals(newStatus)) {

            ticket.setHoldReason("Waiting for employee communication");
            ticket.setHoldStartedAt(LocalDateTime.now());

            slaService.pauseSla(ticketId);

            log.info(
                    "Ticket moved to NEED_EMPLOYEE_COMMUNICATION. SLA paused. ticketId={}",
                    ticketId
            );
        }

        // Agent continues working -> resume SLA
        else if (TicketStatus.IN_PROGRESS.name().equals(newStatus)) {

            ticket.setHoldReason(null);
            ticket.setHoldStartedAt(null);

            slaService.resumeSla(ticketId);

            log.info(
                    "Ticket moved to IN_PROGRESS. SLA resumed. ticketId={}",
                    ticketId
            );
        }

        Ticket savedTicket = ticketRepository.save(ticket);

        log.info(
                "Ticket status updated. ticketId={}, newStatus={}",
                ticketId,
                newStatus
        );

        return ticketMapper.toResponse(savedTicket);
    }

    @Override
    public TicketResponse updatePriority(Long ticketId, UpdateTicketPriorityRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));
        ticket.setPriority(request.getPriority());
        Ticket savedTicket = ticketRepository.save(ticket);
        log.info("Ticket priority updated. ticketId={}, newPriority={}", ticketId, request.getPriority());
        return ticketMapper.toResponse(savedTicket);
    }

    @Override
    public TicketResponse updateCategory(Long ticketId, UpdateTicketCategoryRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + request.getCategoryId()));

        SubCategory subCategory = subCategoryRepository.findById(request.getSubCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("SubCategory not found: " + request.getSubCategoryId()));

        ticket.setCategory(category);
        ticket.setSubCategory(subCategory);
        ticket.setPriority(subCategory.getPriority());

        Ticket savedTicket = ticketRepository.save(ticket);
        log.info("Ticket category updated. ticketId={}, newCategoryId={}", ticketId, request.getCategoryId());
        return ticketMapper.toResponse(savedTicket);
    }


    @Override
    public TicketResponse assignTicket(Long ticketId, AssignTicketRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        DepartmentAgent departmentAgent = departmentAgentRepository.findByEmployeeId(request.getAgentId())
                .orElseThrow(() -> new IllegalArgumentException("DepartmentAgent not found for employee: " + request.getAgentId()));

        ticket.setAssignedAgent(departmentAgent);
        Ticket savedTicket = ticketRepository.save(ticket);
        log.info("Ticket assigned. ticketId={}, agentId={}", ticketId, request.getAgentId());
        return ticketMapper.toResponse(savedTicket);
    }

    @Override
    public TicketResponse resolveTicket(
            Long ticketId,
            UpdateTicketStatusRequest request) {

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Ticket not found: " + ticketId));

        ticket.setStatus(TicketStatus.RESOLVED.name());
        ticket.setResolvedAt(LocalDateTime.now());

        if (request.getResolutionSummary() != null
                && !request.getResolutionSummary().isBlank()) {

            ticket.setResolutionSummary(request.getResolutionSummary());
        }

        slaService.completeSla(ticketId);

        Ticket savedTicket = ticketRepository.save(ticket);

        log.info("Ticket resolved. ticketId={}", ticketId);

        return ticketMapper.toResponse(savedTicket);
    }

    @Override
    public TicketResponse reopenTicket(Long ticketId, ReopenTicketRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));
        ticket.setStatus(TicketStatus.REOPENED.name());
        ticket.setReopenedAt(LocalDateTime.now());
        Ticket savedTicket = ticketRepository.save(ticket);
        log.info("Ticket reopened. ticketId={}", ticketId);
        return ticketMapper.toResponse(savedTicket);
    }

    @Override
    public TicketResponse holdTicket(Long ticketId, HoldTicketRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        if (ticket.getAssignedAgent() == null) {
            throw new IllegalStateException("Ticket must have an assigned agent to be put on hold");
        }

        if (ticket.getHoldStartedAt() != null) {
            throw new IllegalStateException("Ticket is already on hold");
        }

        ticket.setHoldReason(request.getReason());
        ticket.setHoldStartedAt(LocalDateTime.now());
        ticket.setStatus(TicketStatus.NEED_EMPLOYEE_COMMUNICATION.name());

        Ticket savedTicket = ticketRepository.save(ticket);

        slaService.pauseSla(ticketId);

        ticketHistoryService.recordHistory(
                ticket,
                ticket.getAssignedAgent().getEmployee(),
                TicketEventType.SLA_HOLD,
                null,
                "Ticket put on hold: " + request.getReason(),
                null
        );

        notificationService.sendNotification(
                ticket.getRequester(),
                ticket,
                NotificationType.TICKET_HOLD,
                "Ticket On Hold",
                "Your ticket " + ticket.getTicketNumber() + " has been put on hold. Reason: " + request.getReason()
        );

        log.info("Ticket {} put on hold by agent", ticketId);

        return ticketMapper.toResponse(savedTicket);
    }

    @Override
    public TicketResponse resumeTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        if (ticket.getHoldStartedAt() == null) {
            throw new IllegalStateException("Ticket is not on hold");
        }

        slaService.resumeSla(ticketId);

        ticket.setHoldReason(null);
        ticket.setHoldStartedAt(null);
        ticket.setStatus(TicketStatus.IN_PROGRESS.name());

        Ticket savedTicket = ticketRepository.save(ticket);

        ticketHistoryService.recordHistory(
                ticket,
                ticket.getRequester(),
                TicketEventType.SLA_RESUMED,
                null,
                "Ticket resumed by requester response",
                null
        );

        if (ticket.getAssignedAgent() != null) {
            notificationService.sendNotification(
                    ticket.getAssignedAgent().getEmployee(),
                    ticket,
                    NotificationType.TICKET_RESUMED,
                    "Ticket Resumed",
                    "Ticket " + ticket.getTicketNumber() + " has been resumed after requester response"
            );
        }

        log.info("Ticket {} resumed", ticketId);

        return ticketMapper.toResponse(savedTicket);
    }

    @Override
    public TicketResponse resolveTicketWithSummary(Long ticketId, ResolveTicketRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        if (ticket.getAssignedAgent() == null) {
            throw new IllegalStateException("Ticket must have an assigned agent to be resolved");
        }

        if (ticket.getStatus().equals(TicketStatus.RESOLVED.name()) 
                || ticket.getStatus().equals(TicketStatus.CLOSED.name())) {
            throw new IllegalStateException("Ticket is already resolved or closed");
        }

        if (request.getResolutionSummary() == null || request.getResolutionSummary().trim().isEmpty()) {
            throw new IllegalArgumentException("Resolution summary cannot be blank");
        }

        ticket.setStatus(TicketStatus.RESOLVED.name());
        ticket.setResolutionSummary(request.getResolutionSummary().trim());
        ticket.setResolvedAt(LocalDateTime.now());

        Ticket savedTicket = ticketRepository.save(ticket);

        // Check SLA status before completion
        TicketSla ticketSla = ticketSlaRepository.findByTicketId(ticketId);
        boolean slaMet = false;
        if (ticketSla != null && !SlaStatus.BREACHED.name().equals(ticketSla.getStatus())) {
            slaMet = true;
        }

        slaService.completeSla(ticketId);

        ticketHistoryService.recordHistory(
                ticket,
                ticket.getAssignedAgent().getEmployee(),
                TicketEventType.TICKET_RESOLVED,
                null,
                "Ticket resolved: " + request.getResolutionSummary(),
                null
        );

        if (slaMet) {
            ticketHistoryService.recordHistory(
                    ticket,
                    ticket.getAssignedAgent().getEmployee(),
                    TicketEventType.SLA_MET,
                    null,
                    "SLA met - ticket resolved before deadline",
                    null
            );
        }

        notificationService.sendNotification(
                ticket.getRequester(),
                ticket,
                NotificationType.TICKET_RESOLVED,
                "Ticket Resolved",
                "Your ticket " + ticket.getTicketNumber() + " has been resolved. Please provide feedback."
        );

        log.info("Ticket {} resolved with SLA met: {}", ticketId, slaMet);

        return ticketMapper.toResponse(savedTicket);
    }

    @Override
    public TicketResponse reopenTicketWithSla(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        if (!ticket.getStatus().equals(TicketStatus.RESOLVED.name()) 
                && !ticket.getStatus().equals(TicketStatus.CLOSED.name())) {
            throw new IllegalStateException("Ticket must be resolved or closed to be reopened");
        }

        if (ticket.getAssignedAgent() == null) {
            throw new IllegalStateException("Ticket must have an assigned agent to be reopened");
        }

        // Get the original SLA to calculate half duration
        TicketSla originalSla = ticketSlaRepository.findByTicketId(ticketId);
        if (originalSla == null) {
            throw new IllegalStateException("No SLA instance found for ticket");
        }

        int newAllocatedMinutes = Math.max(originalSla.getAllocatedMinutes() / 2, 30); // Minimum 30 minutes

        ticket.setStatus(TicketStatus.REOPENED.name());
        ticket.setReopenCount(ticket.getReopenCount() + 1);
        ticket.setReopenedAt(LocalDateTime.now());
        ticket.setResolvedAt(null);
        ticket.setResolutionSummary(null);

        Ticket savedTicket = ticketRepository.save(ticket);

        // Create new SLA instance with half duration
        slaService.createSlaInstanceForReopen(ticket, newAllocatedMinutes);

        ticketHistoryService.recordHistory(
                ticket,
                ticket.getRequester(),
                TicketEventType.TICKET_REOPENED,
                String.valueOf(ticket.getReopenCount() - 1),
                String.valueOf(ticket.getReopenCount()),
                null
        );

        if (ticket.getAssignedAgent() != null) {
            notificationService.sendNotification(
                    ticket.getAssignedAgent().getEmployee(),
                    ticket,
                    NotificationType.TICKET_REOPENED,
                    "Ticket Reopened",
                    "Ticket " + ticket.getTicketNumber() + " has been reopened. New SLA cycle started."
            );
        }

        log.info("Ticket {} reopened with new SLA cycle, allocated minutes: {}", ticketId, newAllocatedMinutes);

        return ticketMapper.toResponse(savedTicket);
    }
}
