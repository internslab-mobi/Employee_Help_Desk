package com.divya.helpdesk.service.impl;

import com.divya.helpdesk.dto.PageResponse;
import com.divya.helpdesk.dto.ticket.*;
import com.divya.helpdesk.entity.*;
import com.divya.helpdesk.enums.*;
import com.divya.helpdesk.exception.AccessDeniedException;
import com.divya.helpdesk.exception.BadRequestException;
import com.divya.helpdesk.exception.InvalidOperationException;
import com.divya.helpdesk.exception.ResourceNotFoundException;
import com.divya.helpdesk.exception.TicketNotFoundException;
import com.divya.helpdesk.mapper.TicketMapper;
import com.divya.helpdesk.repository.*;
import com.divya.helpdesk.service.CurrentUserService;
import com.divya.helpdesk.service.*;
import com.divya.helpdesk.util.TimezoneUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TicketServiceImpl implements TicketService {

    private final HDTicketRepository ticketRepository;
    private final HDEmployeeRepository employeeRepository;
    private final HDDepartmentRepository departmentRepository;
    private final HDCategoryRepository categoryRepository;
    private final HDSubCategoryRepository subCategoryRepository;
    private final HDSlaPolicyRepository slaPolicyRepository;
    private final HDTicketFeedbackRepository feedbackRepository;
    private final HDSlaInstanceRepository slaInstanceRepository;

    private final CurrentUserService currentUserService;
    private final SlaInstanceService slaInstanceService;
    private final TicketAssignmentService ticketAssignmentService;
    private final TicketHistoryService ticketHistoryService;
    private final TicketMessageService ticketMessageService;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final TicketMapper ticketMapper;

    // Helper for response building with service-level requester timezone conversion
    private CreateTicketResponseDTO buildTicketResponse(HDTicketEntity ticket, boolean includeUpdatedAt) {
        if (ticket == null) {
            return null;
        }

        CreateTicketResponseDTO response = ticketMapper.mapToResponse(ticket);

        String requesterTimezone = (ticket.getRequester() != null && ticket.getRequester().getTimezone() != null && !ticket.getRequester().getTimezone().isBlank()) ? ticket.getRequester().getTimezone() : "UTC";

        response.setCreatedAt(TimezoneUtil.convertToEmployeeTimezone(ticket.getCreatedAt(), requesterTimezone));
        response.setWorkStartedAt(TimezoneUtil.convertToEmployeeTimezone(ticket.getWorkStartedAt(), requesterTimezone));
        response.setResolvedAt(TimezoneUtil.convertToEmployeeTimezone(ticket.getResolvedAt(), requesterTimezone));
        response.setWithdrawnAt(TimezoneUtil.convertToEmployeeTimezone(ticket.getWithdrawnAt(), requesterTimezone));

        if (includeUpdatedAt) {
            response.setUpdatedAt(TimezoneUtil.convertToEmployeeTimezone(ticket.getUpdatedAt(), requesterTimezone));
        }

        if (ticket.getId() != null) {
            slaInstanceRepository.findByTicketId(ticket.getId()).ifPresent(sla -> {
                response.setWarningAt(TimezoneUtil.convertToEmployeeTimezone(sla.getWarningAt(), requesterTimezone));
            });
        }

        return response;
    }

    // 1. CREATE TICKET (Concurrency-Safe Auto-Generated Ticket Number HD_001, HD_002...)
    @Override
    public CreateTicketResponseDTO createTicket(CreateTicketRequestDTO request) {
        HDEmployeeEntity requester = currentUserService.getCurrentEmployee();

        HDDepartmentEntity department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.getDepartmentId()));

        HDCategoryEntity category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));

        HDSubCategoryEntity subCategory = subCategoryRepository.findById(request.getSubCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Sub-category not found with id: " + request.getSubCategoryId()));

        if (!category.getDepartment().getId().equals(department.getId())) {
            throw new BadRequestException("Category '" + category.getName() + "' does not belong to department '" + department.getName() + "'");
        }

        if (!subCategory.getCategory().getId().equals(category.getId())) {
            throw new BadRequestException("Sub-category '" + subCategory.getName() + "' does not belong to category '" + category.getName() + "'");
        }

        // Find active SLA policy for this department + subCategory
        HDSlaPolicyEntity slaPolicy = resolveSlaPolicy(department.getId(), subCategory.getId());

        HDTicketEntity ticket = new HDTicketEntity();
        ticket.setTicketNumber(generateUniqueTicketNumber());
        ticket.setRequester(requester);
        ticket.setDepartment(department);
        ticket.setCategory(category);
        ticket.setSubCategory(subCategory);
        ticket.setDescription(request.getDescription().trim());
        ticket.setPriority(slaPolicy.getPriority() != null ? slaPolicy.getPriority() : TicketPriority.MEDIUM);
        ticket.setStatus(TicketStatus.NEW);
        ticket.setSlaPolicy(slaPolicy);
        ticket.setReopenCount(0);

        HDTicketEntity savedTicket = ticketRepository.save(ticket);

        // Intelligent Agent Assignment (does NOT start SLA)
        HDEmployeeEntity assignedAgent = ticketAssignmentService.assignAgent(savedTicket);
        if (assignedAgent != null) {
            savedTicket.setAssignedAgent(assignedAgent);
            savedTicket = ticketRepository.save(savedTicket);
        }

        // Automated History Logging
        ticketHistoryService.log(savedTicket, requester, TicketEventType.CREATED, null, "Ticket created with status NEW");
        if (assignedAgent != null) {
            ticketHistoryService.log(savedTicket, assignedAgent, TicketEventType.ASSIGNED, null, assignedAgent.getId().toString());
        }

        // Notifications & Emails
        emailService.sendTicketCreatedEmail(savedTicket);
        notificationService.createNotification(requester,
                "Ticket Raised: " + savedTicket.getTicketNumber(),
                "Your ticket has been raised successfully in " + department.getName(),
                NotificationType.TICKET_CREATED, savedTicket);

        if (assignedAgent != null) {
            emailService.sendTicketAssignedEmail(savedTicket, assignedAgent);
            notificationService.createNotification(assignedAgent,
                    "New Ticket Assigned: " + savedTicket.getTicketNumber(),
                    "You have been assigned ticket " + savedTicket.getTicketNumber() + " (" + savedTicket.getPriority() + ")",
                    NotificationType.TICKET_ASSIGNED, savedTicket);
        }

        log.info("Ticket {} created successfully by {}", savedTicket.getTicketNumber(), requester.getEmail());
        return buildTicketResponse(savedTicket, false);
    }

    // START WORKING (Agent starts work, sets workStartedAt, changes status to IN_PROGRESS, starts SLA)
    @Override
    public CreateTicketResponseDTO startWorking(Long ticketId) {
        HDTicketEntity ticket = ticketRepository.findByIdForUpdate(ticketId)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with id: " + ticketId));

        HDEmployeeEntity current = currentUserService.getCurrentEmployee();

        // Verify that the employee is the assigned agent
        if (ticket.getAssignedAgent() == null || !ticket.getAssignedAgent().getId().equals(current.getId())) {
            throw new AccessDeniedException("Only the assigned agent can start working on this ticket");
        }

        // Verify that ticket is allowed to start working
        if (ticket.getStatus() == TicketStatus.RESOLVED || ticket.getStatus() == TicketStatus.WITHDRAWN) {
            throw new InvalidOperationException("Cannot start working on a ticket that is " + ticket.getStatus());
        }

        // Verify workStartedAt is not already set
        if (ticket.getWorkStartedAt() != null) {
            throw new InvalidOperationException("Work has already started on this ticket");
        }

        Instant now = Instant.now();
        ticket.setWorkStartedAt(now);

        TicketStatus oldStatus = ticket.getStatus();
        ticket.setStatus(TicketStatus.IN_PROGRESS);

        HDSlaPolicyEntity slaPolicy = ticket.getSlaPolicy();
        if (slaPolicy == null) {
            slaPolicy = resolveSlaPolicy(ticket.getDepartment().getId(), ticket.getSubCategory().getId());
            ticket.setSlaPolicy(slaPolicy);
        }

        HDTicketEntity savedTicket = ticketRepository.save(ticket);

        // Start SLA instance from workStartedAt
        HDSlaInstanceEntity slaInstance;
        if (savedTicket.getReopenCount() != null && savedTicket.getReopenCount() > 0) {
            slaInstance = slaInstanceService.reopenSlaInstance(savedTicket, slaPolicy);
        } else {
            slaInstance = slaInstanceService.createSlaInstance(savedTicket, slaPolicy);
        }

        ticketHistoryService.log(savedTicket, current, TicketEventType.STATUS_CHANGED,
                oldStatus != null ? oldStatus.name() : null, TicketStatus.IN_PROGRESS.name());
        ticketHistoryService.log(savedTicket, current, TicketEventType.SLA_STARTED,
                null, "Agent started working. SLA started from " + now + ". Deadline: " + slaInstance.getCurrentDeadlineAt());

        emailService.sendTicketStatusChangedEmail(savedTicket, oldStatus, TicketStatus.IN_PROGRESS);
        notificationService.createNotification(
                savedTicket.getRequester(),
                "Agent Started Working: " + savedTicket.getTicketNumber(),
                "The assigned agent has started working on your ticket.",
                NotificationType.STATUS_CHANGED,
                savedTicket
        );

        log.info("Agent {} started working on ticket {}", current.getEmail(), savedTicket.getTicketNumber());
        return buildTicketResponse(savedTicket, true);
    }

    // 2. FLEXIBLE SINGLE PATCH ENDPOINT (PATCH /api/tickets/{id})
    @Override
    public CreateTicketResponseDTO patchTicket(Long ticketId, TicketPatchRequestDTO request) {
        HDTicketEntity ticket = getTicketEntity(ticketId);
        HDEmployeeEntity current = currentUserService.getCurrentEmployee();

        // 1. Authorization validation
        validateCanManageAssignedTicket(ticket, current);

        // 2. Resolve field values (if null in request, keep existing)
        HDDepartmentEntity department = (request.getDepartmentId() != null)
                ? departmentRepository.findById(request.getDepartmentId())
                        .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.getDepartmentId()))
                : ticket.getDepartment();

        HDCategoryEntity category = (request.getCategoryId() != null)
                ? categoryRepository.findById(request.getCategoryId())
                        .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()))
                : ticket.getCategory();

        HDSubCategoryEntity subCategory = (request.getSubCategoryId() != null)
                ? subCategoryRepository.findById(request.getSubCategoryId())
                        .orElseThrow(() -> new ResourceNotFoundException("Sub-category not found with id: " + request.getSubCategoryId()))
                : ticket.getSubCategory();

        // 3. Validate classification relationships
        validateClassification(department, category, subCategory);

        // 4. Capture old values for change detection & history logging
        HDDepartmentEntity oldDepartment = ticket.getDepartment();
        HDCategoryEntity oldCategory = ticket.getCategory();
        HDSubCategoryEntity oldSubCategory = ticket.getSubCategory();
        TicketPriority oldPriority = ticket.getPriority();

        boolean departmentChanged = (oldDepartment == null && department != null)
                || (oldDepartment != null && department != null && !oldDepartment.getId().equals(department.getId()));

        boolean categoryChanged = (oldCategory == null && category != null)
                || (oldCategory != null && category != null && !oldCategory.getId().equals(category.getId()));

        boolean subCategoryChanged = (oldSubCategory == null && subCategory != null)
                || (oldSubCategory != null && subCategory != null && !oldSubCategory.getId().equals(subCategory.getId()));

        boolean classificationChanged = departmentChanged || categoryChanged || subCategoryChanged;

        // 5. Handle Description update
        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            String newDescription = request.getDescription().trim();
            String oldDescription = ticket.getDescription();
            if (!newDescription.equals(oldDescription)) {
                ticket.setDescription(newDescription);
                ticketHistoryService.log(ticket, current, TicketEventType.STATUS_CHANGED, oldDescription, newDescription);
            }
        }

        // 6. Handle Classification Change (SLA derivation, priority derivation, SLA recalculation, Reassignment)
        if (classificationChanged) {
            ticket.setDepartment(department);
            ticket.setCategory(category);
            ticket.setSubCategory(subCategory);

            // Resolve SLA Policy
            HDSlaPolicyEntity newPolicy = resolveSlaPolicy(department.getId(), subCategory.getId());
            TicketPriority newPriority = newPolicy.getPriority() != null ? newPolicy.getPriority() : TicketPriority.MEDIUM;
            ticket.setPriority(newPriority);
            ticket.setSlaPolicy(newPolicy);

            // Recalculate SLA instance using business calendar if work has started
            recalculateTicketSla(ticket, newPolicy);

            // Re-evaluate Agent Assignment
            reassignIfRequired(ticket, current);

            // Record history only for fields that actually changed
            if (departmentChanged) {
                ticketHistoryService.log(ticket, current, TicketEventType.STATUS_CHANGED,
                        oldDepartment != null ? oldDepartment.getName() : null,
                        department.getName());
            }
            if (categoryChanged) {
                ticketHistoryService.log(ticket, current, TicketEventType.STATUS_CHANGED,
                        oldCategory != null ? oldCategory.getName() : null,
                        category.getName());
            }
            if (subCategoryChanged) {
                ticketHistoryService.log(ticket, current, TicketEventType.STATUS_CHANGED,
                        oldSubCategory != null ? oldSubCategory.getName() : null,
                        subCategory.getName());
            }
            if (oldPriority != newPriority) {
                ticketHistoryService.log(ticket, current, TicketEventType.PRIORITY_CHANGED,
                        oldPriority != null ? oldPriority.name() : null,
                        newPriority.name());
            }
        }

        HDTicketEntity saved = ticketRepository.save(ticket);
        log.info("Ticket {} patched successfully by {}", saved.getTicketNumber(), current.getEmail());

        return buildTicketResponse(saved, true);
    }

    // 3. UPDATE TICKET (PUT)
    @Override
    public CreateTicketResponseDTO updateTicket(Long ticketId, UpdateTicketRequestDTO request) {
        HDTicketEntity ticket = getTicketEntity(ticketId);
        HDEmployeeEntity current = currentUserService.getCurrentEmployee();

        boolean isRequester = ticket.getRequester().getId().equals(current.getId());

        if (!isRequester) {
            throw new AccessDeniedException("You are not authorized to update this ticket");
        }

        if (ticket.getStatus() != TicketStatus.NEW && ticket.getStatus() != TicketStatus.REOPENED) {
            throw new InvalidOperationException("Ticket can only be edited while in NEW or REOPENED status");
        }

        HDDepartmentEntity department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.getDepartmentId()));
        HDCategoryEntity category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));
        HDSubCategoryEntity subCategory = subCategoryRepository.findById(request.getSubCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Sub-category not found with id: " + request.getSubCategoryId()));

        boolean deptOrSubCategoryChanged = !ticket.getDepartment().getId().equals(department.getId())
                || !ticket.getSubCategory().getId().equals(subCategory.getId());

        ticket.setDepartment(department);
        ticket.setCategory(category);
        ticket.setSubCategory(subCategory);
        ticket.setDescription(request.getDescription().trim());

        if (deptOrSubCategoryChanged) {
            // Recalculate SLA Policy and Deadline
            HDSlaPolicyEntity newPolicy = resolveSlaPolicy(department.getId(), subCategory.getId());
            ticket.setSlaPolicy(newPolicy);
            ticket.setPriority(newPolicy.getPriority() != null ? newPolicy.getPriority() : ticket.getPriority());

            if (ticket.getWorkStartedAt() != null) {
                slaInstanceService.createSlaInstance(ticket, newPolicy);
            }

            // Re-evaluate Agent Assignment
            HDEmployeeEntity oldAgent = employeeRepository.findById(current.getId()).orElse(null);
            HDEmployeeEntity newAgent = ticketAssignmentService.assignAgent(ticket);
            if (newAgent != null && (ticket.getAssignedAgent() == null || !ticket.getAssignedAgent().getId().equals(newAgent.getId()))) {
                ticket.setAssignedAgent(newAgent);
                ticketHistoryService.log(ticket, current, TicketEventType.REASSIGNED, oldAgent.toString(), newAgent.getId().toString());
            }
        }

        HDTicketEntity updated = ticketRepository.save(ticket);
        ticketHistoryService.log(ticket, current, TicketEventType.STATUS_CHANGED, null, "Ticket details updated");

        return buildTicketResponse(updated, true);
    }

    // 4. DELETE TICKET (MANAGER / ADMIN)
    @Override
    public void deleteTicket(Long ticketId) {
        HDTicketEntity ticket = getTicketEntity(ticketId);
        HDEmployeeEntity current = currentUserService.getCurrentEmployee();

        boolean isManager = current.getRole() == EmployeeRole.MANAGER
                && ticket.getDepartment() != null
                && current.getDepartment() != null
                && ticket.getDepartment().getId().equals(current.getDepartment().getId());
        boolean isAdmin = current.getRole() == EmployeeRole.ADMIN;

        if (!isManager && !isAdmin) {
            throw new AccessDeniedException("Only department manager or admin can delete tickets");
        }

        ticketRepository.delete(ticket);
        log.warn("Ticket {} deleted by {}", ticket.getTicketNumber(), current.getEmail());
    }

    // 5. GET MY TICKETS (EMPLOYEE)
    @Override
    @Transactional(readOnly = true)
    public List<CreateTicketResponseDTO> getMyTickets(TicketStatus status) {
        Long employeeId = currentUserService.getEmployeeId();
        List<HDTicketEntity> tickets = (status == null)
                ? ticketRepository.findByRequesterIdOrderByCreatedAtDesc(employeeId)
                : ticketRepository.findByRequesterIdAndStatusOrderByCreatedAtDesc(employeeId, status);

        return tickets.stream()
                .map(t -> buildTicketResponse(t, false))
                .toList();
    }

    // 7. GET ASSIGNED TICKETS (AGENT / MANAGER)
    @Override
    @Transactional(readOnly = true)
    public List<CreateTicketResponseDTO> getAssignedTickets(TicketStatus status) {
        Long currentUserId = currentUserService.getEmployeeId();
        HDEmployeeEntity current = currentUserService.getCurrentEmployee();

        List<HDTicketEntity> tickets;
        if (current.getRole() == EmployeeRole.MANAGER) {
            tickets = (status == null)
                    ? ticketRepository.findByAssignedManagerIdOrderByCreatedAtDesc(currentUserId)
                    : ticketRepository.findByAssignedManagerIdAndStatusOrderByCreatedAtDesc(currentUserId, status);
        } else {
            tickets = (status == null)
                    ? ticketRepository.findByAssignedAgentIdOrderByCreatedAtDesc(currentUserId)
                    : ticketRepository.findByAssignedAgentIdAndStatusOrderByCreatedAtDesc(currentUserId, status);
        }

        return tickets.stream().map(t -> buildTicketResponse(t, false)).toList();
    }

    // 8. GET DEPARTMENT TICKETS (MANAGER)
    @Override
    @Transactional(readOnly = true)
    public PageResponse<CreateTicketResponseDTO> getDepartmentTickets(Long agentId, TicketStatus status, int limit, long offset) {
        int safeLimit = (limit <= 0) ? 10 : Math.min(limit, 50);
        if (offset < 0) {
            throw new BadRequestException("Offset cannot be negative");
        }

        HDEmployeeEntity current = currentUserService.getCurrentEmployee();
        if (current.getDepartment() == null) {
            throw new BadRequestException("Manager does not have an assigned department");
        }

        Long departmentId = current.getDepartment().getId();
        int pageIndex = (int) (offset / safeLimit);
        Pageable pageable = PageRequest.of(pageIndex, safeLimit, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<HDTicketEntity> page;
        if (agentId != null && status != null) {
            page = ticketRepository.findByDepartmentIdAndAssignedAgentIdAndStatus(departmentId, agentId, status, pageable);
        } else if (agentId != null) {
            page = ticketRepository.findByDepartmentIdAndAssignedAgentId(departmentId, agentId, pageable);
        } else if (status != null) {
            page = ticketRepository.findByDepartmentIdAndStatus(departmentId, status, pageable);
        } else {
            page = ticketRepository.findByDepartmentId(departmentId, pageable);
        }

        List<CreateTicketResponseDTO> content = page.getContent()
                .stream()
                .map(t -> buildTicketResponse(t, false))
                .toList();

        return PageResponse.<CreateTicketResponseDTO>builder()
                .content(content)
                .limit(safeLimit)
                .offset(offset)
                .totalElements(page.getTotalElements())
                .hasNext(page.hasNext())
                .build();
    }

    // 9. WITHDRAW TICKET (EMPLOYEE)
    @Override
    public void withdrawTicket(Long ticketId, WithdrawTicketRequestDTO request) {
        HDTicketEntity ticket = getTicketEntity(ticketId);
        HDEmployeeEntity current = currentUserService.getCurrentEmployee();

        if (!ticket.getRequester().getId().equals(current.getId())) {
            throw new AccessDeniedException("You can only withdraw your own ticket");
        }

        if (ticket.getStatus() == TicketStatus.RESOLVED || ticket.getStatus() == TicketStatus.WITHDRAWN) {
            throw new InvalidOperationException("Cannot withdraw ticket in status: " + ticket.getStatus());
        }

        TicketStatus oldStatus = ticket.getStatus();
        ticket.setStatus(TicketStatus.WITHDRAWN);
        ticket.setWithdrawalReason(request.getReason());
        ticket.setWithdrawnAt(Instant.now());

        ticketRepository.save(ticket);
        slaInstanceService.resolveSlaInstance(ticket);

        ticketHistoryService.log(ticket, current, TicketEventType.WITHDRAWN, oldStatus.name(), request.getReason());
        log.info("Ticket {} withdrawn by requester {}", ticket.getTicketNumber(), current.getEmail());
    }


    // 10. RESOLVE TICKET
    @Override
    public CreateTicketResponseDTO resolveTicket(Long ticketId, ResolveTicketRequestDTO request) {
        HDTicketEntity ticket = getTicketEntity(ticketId);
        HDEmployeeEntity current = currentUserService.getCurrentEmployee();

        validateCanResolve(ticket, current);

        if (ticket.getStatus() == TicketStatus.RESOLVED || ticket.getStatus() == TicketStatus.WITHDRAWN) {
            throw new InvalidOperationException("Ticket is already " + ticket.getStatus());
        }

        TicketStatus oldStatus = ticket.getStatus();
        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setResolutionSummary(request.getResolutionSummary().trim());
        ticket.setResolvedAt(Instant.now());

        HDTicketEntity saved = ticketRepository.save(ticket);
        slaInstanceService.resolveSlaInstance(saved);

        ticketHistoryService.log(saved, current, TicketEventType.STATUS_CHANGED, oldStatus.name(), TicketStatus.RESOLVED.name());
        ticketHistoryService.log(saved, current, TicketEventType.RESOLVED, null, request.getResolutionSummary());

        emailService.sendTicketStatusChangedEmail(saved, oldStatus, TicketStatus.RESOLVED);
        notificationService.createNotification(
                saved.getRequester(),
                "Ticket Resolved: " + saved.getTicketNumber(),
                "Your ticket has been marked resolved. Summary: " + request.getResolutionSummary(),
                NotificationType.TICKET_RESOLVED, saved);

        log.info("Ticket {} resolved by {}", saved.getTicketNumber(), current.getEmail());
        return buildTicketResponse(saved, true);
    }

    // 11. WAITING FOR EMPLOYEE
    @Override
    public CreateTicketResponseDTO waitingForEmployee(Long ticketId, WaitingForEmployeeRequestDTO request) {
        HDTicketEntity ticket = getTicketEntity(ticketId);
        HDEmployeeEntity current = currentUserService.getCurrentEmployee();

        validateCanManageAssignedTicket(ticket, current);

        if (ticket.getStatus() == TicketStatus.RESOLVED || ticket.getStatus() == TicketStatus.WITHDRAWN) {
            throw new InvalidOperationException("Cannot set waiting status on a closed ticket");
        }

        TicketStatus oldStatus = ticket.getStatus();
        ticket.setStatus(TicketStatus.WAITING_FOR_EMPLOYEE);
        ticket.setHoldReason(request.getReason());
        ticket.setHoldStartedAt(Instant.now());

        HDTicketEntity saved = ticketRepository.save(ticket);
        slaInstanceService.pauseSlaInstance(saved);

        ticketHistoryService.log(
                saved,
                current,
                TicketEventType.STATUS_CHANGED,
                oldStatus.name(),
                TicketStatus.WAITING_FOR_EMPLOYEE.name()
        );

        notificationService.createNotification(
                saved.getRequester(),
                "Action Required on Ticket " + saved.getTicketNumber(),
                "Agent is waiting for your response: " + request.getReason(),
                NotificationType.WAITING_FOR_EMPLOYEE,
                saved
        );

        return buildTicketResponse(saved, true);
    }

    // 12. RESUME TICKET (AGENT / MANAGER)
    @Override
    public CreateTicketResponseDTO resumeTicket(Long ticketId) {
        HDTicketEntity ticket = getTicketEntity(ticketId);
        HDEmployeeEntity current = currentUserService.getCurrentEmployee();

        validateCanManageAssignedTicket(ticket, current);

        if (ticket.getStatus() != TicketStatus.WAITING_FOR_EMPLOYEE) {
            throw new InvalidOperationException("Ticket is not currently in WAITING_FOR_EMPLOYEE status");
        }

        TicketStatus oldStatus = ticket.getStatus();
        slaInstanceService.resumeSlaInstance(ticket);

        ticket.setStatus(TicketStatus.IN_PROGRESS);
        ticket.setHoldReason(null);
        ticket.setHoldStartedAt(null);

        HDTicketEntity saved = ticketRepository.save(ticket);

        ticketHistoryService.log(
                saved,
                current,
                TicketEventType.STATUS_CHANGED,
                oldStatus.name(),
                TicketStatus.IN_PROGRESS.name()
        );

        return buildTicketResponse(saved, true);
    }

    // 13. REOPEN TICKET (EMPLOYEE - 50% SLA ALLOCATION)
    @Override
    public CreateTicketResponseDTO reopenTicket(Long ticketId, ReopenTicketRequestDTO request) {
        HDTicketEntity ticket = getTicketEntity(ticketId);
        HDEmployeeEntity current = currentUserService.getCurrentEmployee();

        if (!ticket.getRequester().getId().equals(current.getId())) {
            throw new AccessDeniedException("Only the ticket requester can reopen this ticket");
        }

        if (ticket.getStatus() != TicketStatus.RESOLVED) {
            throw new InvalidOperationException("Only resolved tickets can be reopened");
        }

        int currentReopenCount = ticket.getReopenCount() != null ? ticket.getReopenCount() : 0;
        if (currentReopenCount >= 2) {
            throw new InvalidOperationException("Ticket has reached maximum allowed reopen limit of 2");
        }

        ticket.setReopenCount(currentReopenCount + 1);
        ticket.setStatus(TicketStatus.REOPENED);
        ticket.setResolvedAt(null);
        ticket.setWorkStartedAt(null);

        HDTicketEntity saved = ticketRepository.save(ticket);

        ticketHistoryService.log(
                saved,
                current,
                TicketEventType.REOPENED,
                TicketStatus.RESOLVED.name(),
                "Reopened (Cycle " + ticket.getReopenCount() + "): " + request.getReason()
        );

        emailService.sendTicketStatusChangedEmail(saved, TicketStatus.RESOLVED, TicketStatus.REOPENED);
        if (saved.getAssignedAgent() != null) {
            notificationService.createNotification(
                    saved.getAssignedAgent(),
                    "Ticket Reopened: " + saved.getTicketNumber(),
                    "Requester reopened ticket. Reason: " + request.getReason(),
                    NotificationType.TICKET_REOPENED,
                    saved
            );
        }

        log.info("Ticket {} reopened by requester {}", saved.getTicketNumber(), current.getEmail());
        return buildTicketResponse(saved, true);
    }

    // 14. SUBMIT FEEDBACK (EMPLOYEE)
    @Override
    public void submitFeedback(Long ticketId, FeedbackRequestDTO request) {
        HDTicketEntity ticket = getTicketEntity(ticketId);
        HDEmployeeEntity current = currentUserService.getCurrentEmployee();

        if (!ticket.getRequester().getId().equals(current.getId())) {
            throw new AccessDeniedException("Only the requester can submit feedback");
        }

        if (ticket.getStatus() != TicketStatus.RESOLVED) {
            throw new InvalidOperationException("Feedback can only be submitted for RESOLVED tickets");
        }

        if (feedbackRepository.existsByTicketId(ticketId)) {
            throw new InvalidOperationException("Feedback has already been submitted for this ticket");
        }

        HDTicketFeedbackEntity feedback = new HDTicketFeedbackEntity();
        feedback.setTicket(ticket);
        feedback.setSubmittedBy(current);
        feedback.setRating(request.getRating());

        feedbackRepository.save(feedback);
        log.info("Feedback rating {} submitted for ticket {}", request.getRating(), ticket.getTicketNumber());
    }

    // 15. SEND MESSAGE
    @Override
    public TicketMessageResponseDTO sendMessage(Long ticketId, SendMessageRequestDTO request) {
        HDTicketEntity ticket = getTicketEntity(ticketId);
        HDEmployeeEntity current = currentUserService.getCurrentEmployee();

        validateCanViewTicket(ticket, current);
        return mapToResponse(ticketMessageService.sendMessage(ticket, current, request.getMessage()));
    }


    // HELPER & SECURITY VALIDATION METHODS
    private HDTicketEntity getTicketEntity(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with id: " + ticketId));
    }

    private void validateCanViewTicket(HDTicketEntity ticket, HDEmployeeEntity current) {
        if (current.getRole() == EmployeeRole.ADMIN) {
            return;
        }

        boolean isRequester = ticket.getRequester() != null && ticket.getRequester().getId().equals(current.getId());
        boolean isAssignedAgent = ticket.getAssignedAgent() != null && ticket.getAssignedAgent().getId().equals(current.getId());
        boolean isAssignedManager = ticket.getAssignedManager() != null && ticket.getAssignedManager().getId().equals(current.getId());
        boolean isDeptManager = current.getRole() == EmployeeRole.MANAGER
                && ticket.getDepartment() != null
                && current.getDepartment() != null
                && ticket.getDepartment().getId().equals(current.getDepartment().getId());

        if (!isRequester && !isAssignedAgent && !isAssignedManager && !isDeptManager) {
            throw new AccessDeniedException("You are not authorized to access this ticket");
        }
    }

    private void validateCanResolve(HDTicketEntity ticket, HDEmployeeEntity current) {
        if (current.getRole() == EmployeeRole.ADMIN) {
            return;
        }

        boolean isAssignedAgent = ticket.getAssignedAgent() != null && ticket.getAssignedAgent().getId().equals(current.getId());
        boolean isAssignedManager = ticket.getAssignedManager() != null && ticket.getAssignedManager().getId().equals(current.getId());
        boolean isDeptManager = current.getRole() == EmployeeRole.MANAGER
                && ticket.getDepartment() != null
                && current.getDepartment() != null
                && ticket.getDepartment().getId().equals(current.getDepartment().getId());

        if (!isAssignedAgent && !isAssignedManager && !isDeptManager) {
            throw new AccessDeniedException("You are not authorized to resolve this ticket");
        }
    }

    private void validateCanManageAssignedTicket(HDTicketEntity ticket, HDEmployeeEntity current) {
        if (current.getRole() == EmployeeRole.ADMIN) {
            return;
        }

        boolean isRequester = ticket.getRequester() != null && ticket.getRequester().getId().equals(current.getId());
        boolean isAssignedAgent = ticket.getAssignedAgent() != null && ticket.getAssignedAgent().getId().equals(current.getId());
        boolean isAssignedManager = ticket.getAssignedManager() != null && ticket.getAssignedManager().getId().equals(current.getId());
        boolean isDeptManager = current.getRole() == EmployeeRole.MANAGER
                && ticket.getDepartment() != null
                && current.getDepartment() != null
                && ticket.getDepartment().getId().equals(current.getDepartment().getId());

        if (!isRequester && !isAssignedAgent && !isAssignedManager && !isDeptManager) {
            throw new AccessDeniedException("You are not authorized to manage this ticket");
        }
    }

    private HDSlaPolicyEntity resolveSlaPolicy(Long departmentId, Long subCategoryId) {

        return slaPolicyRepository.findByDepartmentIdAndSubCategoryIdAndActiveTrue(departmentId, subCategoryId)
                .orElseThrow(() -> new InvalidOperationException("No active SLA policy configured for the selected department and sub-category"));
    }

    private synchronized String generateUniqueTicketNumber() {
        Pageable topOne = PageRequest.of(0, 1);
        List<HDTicketEntity> lastList = ticketRepository.findLastTicketForUpdate(topOne);
        long nextNumber = 1;
        if (!lastList.isEmpty() && lastList.get(0).getTicketNumber() != null) {
            nextNumber = extractNextNumber(lastList.get(0).getTicketNumber());
        }

        String code = String.format("HD_%03d", nextNumber);
        while (ticketRepository.existsByTicketNumber(code)) {
            nextNumber++;
            code = String.format("HD_%03d", nextNumber);
        }

        return code;
    }

    private long extractNextNumber(String code) {
        if (code == null || code.isBlank()) {
            return 1;
        }
        Matcher matcher = Pattern.compile("(\\d+)").matcher(code);
        long max = 0;
        while (matcher.find()) {
            try {
                long val = Long.parseLong(matcher.group(1));
                if (val > max) {
                    max = val;
                }
            } catch (NumberFormatException ignored) {}
        }
        return max > 0 ? max + 1 : 1;
    }

    private TicketMessageResponseDTO mapToResponse(HDTicketMessageEntity message) {
        return TicketMessageResponseDTO.builder()
                .id(message.getId())
                .ticketId(message.getTicket() != null ? message.getTicket().getId() : null)
                .sender(mapToSender(message.getSender()))
                .messageText(message.getMessageText())
                .createdAt(message.getCreatedAt().atZone(ZoneId.of(message.getSender().getTimezone())).toOffsetDateTime())
                .build();
    }

    private void validateClassification(HDDepartmentEntity department, HDCategoryEntity category, HDSubCategoryEntity subCategory) {
        if (department == null) {
            throw new BadRequestException("Department is required");
        }
        if (category == null) {
            throw new BadRequestException("Category is required");
        }
        if (subCategory == null) {
            throw new BadRequestException("Sub-category is required");
        }
        if (category.getDepartment() == null || !category.getDepartment().getId().equals(department.getId())) {
            throw new BadRequestException("Category '" + category.getName() + "' does not belong to department '" + department.getName() + "'");
        }
        if (subCategory.getCategory() == null || !subCategory.getCategory().getId().equals(category.getId())) {
            throw new BadRequestException("Sub-category '" + subCategory.getName() + "' does not belong to category '" + category.getName() + "'");
        }
    }

    private void recalculateTicketSla(HDTicketEntity ticket, HDSlaPolicyEntity newPolicy) {
        if (ticket.getWorkStartedAt() != null) {
            slaInstanceService.createSlaInstance(ticket, newPolicy);
        }
    }

    private void reassignIfRequired(HDTicketEntity ticket, HDEmployeeEntity current) {
        HDEmployeeEntity oldAgent = ticket.getAssignedAgent();
        HDEmployeeEntity newAgent = ticketAssignmentService.assignAgent(ticket);

        if (newAgent != null) {
            if (oldAgent == null || !oldAgent.getId().equals(newAgent.getId())) {
                ticket.setAssignedAgent(newAgent);
                TicketEventType eventType = (oldAgent == null) ? TicketEventType.ASSIGNED : TicketEventType.REASSIGNED;
                String oldAgentVal = (oldAgent != null) ? oldAgent.getId().toString() : null;
                ticketHistoryService.log(ticket, current, eventType, oldAgentVal, newAgent.getId().toString());

                emailService.sendTicketAssignedEmail(ticket, newAgent);
                notificationService.createNotification(
                        newAgent,
                        "Assigned Ticket: " + ticket.getTicketNumber(),
                        "You have been assigned ticket " + ticket.getTicketNumber() + " (" + ticket.getPriority() + ")",
                        NotificationType.TICKET_ASSIGNED,
                        ticket
                );
            }
        } else if (oldAgent != null) {
            ticket.setAssignedAgent(null);
            ticketHistoryService.log(ticket, current, TicketEventType.REASSIGNED, oldAgent.getId().toString(), null);
        }
    }

    private MessageSenderDTO mapToSender(HDEmployeeEntity employee) {
        if (employee == null) return null;

        return MessageSenderDTO.builder()
                .id(employee.getId())
                .name(employee.getFirstName() + " " + employee.getLastName())
                .role(employee.getRole().toString())
                .build();
    }

}