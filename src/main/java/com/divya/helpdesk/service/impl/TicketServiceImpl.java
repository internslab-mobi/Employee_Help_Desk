package com.divya.helpdesk.service.impl;

import com.divya.helpdesk.dto.common.PageResponse;
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
import com.divya.helpdesk.security.CurrentUserService;
import com.divya.helpdesk.service.*;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
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

    private final CurrentUserService currentUserService;
    private final HDSlaInstanceService slaInstanceService;
    private final HDTicketAssignmentService ticketAssignmentService;
    private final TicketHistoryService ticketHistoryService;
    private final TicketMessageService ticketMessageService;
    private final EmailService emailService;
    private final NotificationService notificationService;

    // 1. CREATE TICKET (Concurrency-Safe Auto-Generated Ticket Number HD_001, HD_002...)
    @Override
    public TicketResponse createTicket(CreateTicketRequest request) {
        HDEmployee requester = currentUserService.getCurrentEmployee();

        HDDepartment department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.getDepartmentId()));

        HDCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));

        HDSubCategory subCategory = subCategoryRepository.findById(request.getSubCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Sub-category not found with id: " + request.getSubCategoryId()));

        if (!category.getDepartment().getId().equals(department.getId())) {
            throw new BadRequestException("Category '" + category.getName() + "' does not belong to department '" + department.getName() + "'");
        }

        if (!subCategory.getCategory().getId().equals(category.getId())) {
            throw new BadRequestException("Sub-category '" + subCategory.getName() + "' does not belong to category '" + category.getName() + "'");
        }

        // Find active SLA policy for this department + subCategory
        HDSlaPolicy slaPolicy = resolveSlaPolicy(department.getId(), subCategory.getId());

        HDTicket ticket = new HDTicket();
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

        HDTicket savedTicket = ticketRepository.save(ticket);

        // 1. Initialize SLA Instance
        HDSlaInstance slaInstance = slaInstanceService.createSlaInstance(savedTicket, slaPolicy);

        // 2. Intelligent Agent Assignment
        HDEmployee assignedAgent = ticketAssignmentService.assignAgent(savedTicket);
        if (assignedAgent != null) {
            savedTicket.setAssignedAgent(assignedAgent);
            savedTicket = ticketRepository.save(savedTicket);
        }

        // 3. Automated History Logging
        ticketHistoryService.log(savedTicket, requester, TicketEventType.CREATED, null, "Ticket created with status NEW");
        if (assignedAgent != null) {
            ticketHistoryService.log(savedTicket, null, TicketEventType.ASSIGNED, null, assignedAgent.getId().toString());
        }
        ticketHistoryService.log(savedTicket, null, TicketEventType.SLA_STARTED, null, "SLA started. Deadline: " + slaInstance.getCurrentDeadlineAt());

        // 4. Notifications & Emails
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
        return TicketMapper.mapToResponse(savedTicket, false);
    }

    // 2. FLEXIBLE SINGLE PATCH ENDPOINT (PATCH /api/tickets/{id})
    @Override
    public TicketResponse patchTicket(Long ticketId, TicketPatchRequest request) {
        HDTicket ticket = getTicketEntity(ticketId);
        HDEmployee current = currentUserService.getCurrentEmployee();

        validateCanManageAssignedTicket(ticket, current);

        // Update description if supplied
        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            ticket.setDescription(request.getDescription().trim());
            ticketHistoryService.log(ticket, current, TicketEventType.STATUS_CHANGED, null, "Description updated via PATCH");
        }

        // Update priority if supplied
        if (request.getPriority() != null && request.getPriority() != ticket.getPriority()) {
            TicketPriority oldPriority = ticket.getPriority();
            ticket.setPriority(request.getPriority());
            ticketHistoryService.log(ticket, current, TicketEventType.STATUS_CHANGED,
                    oldPriority != null ? oldPriority.name() : null, request.getPriority().name());
        }

        // Update status if supplied
        if (request.getStatus() != null && request.getStatus() != ticket.getStatus()) {
            TicketStatus oldStatus = ticket.getStatus();
            TicketStatus newStatus = request.getStatus();

            ticket.setStatus(newStatus);
            if (newStatus == TicketStatus.RESOLVED) {
                ticket.setResolvedAt(Instant.now());
                slaInstanceService.resolveSlaInstance(ticket);
            } else if (newStatus == TicketStatus.WITHDRAWN) {
                ticket.setWithdrawnAt(Instant.now());
                slaInstanceService.resolveSlaInstance(ticket);
            } else if (newStatus == TicketStatus.WAITING_FOR_EMPLOYEE) {
                ticket.setHoldStartedAt(Instant.now());
            } else if (newStatus == TicketStatus.IN_PROGRESS && oldStatus == TicketStatus.WAITING_FOR_EMPLOYEE) {
                ticket.setHoldReason(null);
                ticket.setHoldStartedAt(null);
            }

            ticketHistoryService.log(ticket, current, TicketEventType.STATUS_CHANGED, oldStatus.name(), newStatus.name());
            emailService.sendTicketStatusChangedEmail(ticket, oldStatus, newStatus);
            notificationService.createNotification(
                    ticket.getRequester(),
                    "Ticket " + ticket.getTicketNumber() + " Status: " + newStatus,
                    "Your ticket status changed from " + oldStatus + " to " + newStatus,
                    NotificationType.STATUS_CHANGED,
                    ticket
            );
        }

        // Update assigned agent if supplied (Manager / Admin only)
        if (request.getAssignedAgentId() != null) {
            if (current.getRole() != EmployeeRole.MANAGER && current.getRole() != EmployeeRole.ADMIN) {
                throw new AccessDeniedException("Only managers or administrators can reassign tickets");
            }
            HDEmployee newAgent = employeeRepository.findById(request.getAssignedAgentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Agent not found with id: " + request.getAssignedAgentId()));
            ticket.setAssignedAgent(newAgent);
            ticketHistoryService.log(ticket, current, TicketEventType.ASSIGNED, null, newAgent.getId().toString());
            emailService.sendTicketAssignedEmail(ticket, newAgent);
            notificationService.createNotification(
                    newAgent,
                    "Assigned Ticket: " + ticket.getTicketNumber(),
                    "You have been assigned ticket " + ticket.getTicketNumber(),
                    NotificationType.TICKET_ASSIGNED,
                    ticket
            );
        }

        if (request.getAssignedManagerId() != null) {
            if (current.getRole() == EmployeeRole.ADMIN) {
                HDEmployee newManager = employeeRepository.findById(request.getAssignedManagerId())
                        .orElseThrow(() -> new ResourceNotFoundException("Manager not found with id: " + request.getAssignedManagerId()));
                ticket.setAssignedManager(newManager);
            }
        }

        if (request.getResolutionSummary() != null && !request.getResolutionSummary().isBlank()) {
            ticket.setResolutionSummary(request.getResolutionSummary().trim());
        }
        if (request.getHoldReason() != null && !request.getHoldReason().isBlank()) {
            ticket.setHoldReason(request.getHoldReason().trim());
        }
        if (request.getWithdrawalReason() != null && !request.getWithdrawalReason().isBlank()) {
            ticket.setWithdrawalReason(request.getWithdrawalReason().trim());
        }

        HDTicket saved = ticketRepository.save(ticket);
        log.info("Ticket {} patched successfully by {}", saved.getTicketNumber(), current.getEmail());

        return TicketMapper.mapToResponse(saved, true);
    }

    // 3. UPDATE TICKET (PUT)
    @Override
    public TicketResponse updateTicket(Long ticketId, UpdateTicketRequest request) {
        HDTicket ticket = getTicketEntity(ticketId);
        HDEmployee current = currentUserService.getCurrentEmployee();

        boolean isRequester = ticket.getRequester().getId().equals(current.getId());
        boolean isManager = current.getRole() == EmployeeRole.MANAGER
                && ticket.getDepartment() != null
                && current.getDepartment() != null
                && ticket.getDepartment().getId().equals(current.getDepartment().getId());
        boolean isAdmin = current.getRole() == EmployeeRole.ADMIN;

        if (!isRequester && !isManager && !isAdmin) {
            throw new AccessDeniedException("You are not authorized to update this ticket");
        }

        if (isRequester && ticket.getStatus() != TicketStatus.NEW && ticket.getStatus() != TicketStatus.REOPENED) {
            throw new InvalidOperationException("Ticket can only be edited while in NEW or REOPENED status");
        }

        HDDepartment department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.getDepartmentId()));
        HDCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));
        HDSubCategory subCategory = subCategoryRepository.findById(request.getSubCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Sub-category not found with id: " + request.getSubCategoryId()));

        boolean deptOrSubCategoryChanged = !ticket.getDepartment().getId().equals(department.getId())
                || !ticket.getSubCategory().getId().equals(subCategory.getId());

        ticket.setDepartment(department);
        ticket.setCategory(category);
        ticket.setSubCategory(subCategory);
        ticket.setDescription(request.getDescription().trim());

        if (deptOrSubCategoryChanged) {
            // Recalculate SLA Policy and Deadline
            HDSlaPolicy newPolicy = resolveSlaPolicy(department.getId(), subCategory.getId());
            ticket.setSlaPolicy(newPolicy);
            ticket.setPriority(newPolicy.getPriority() != null ? newPolicy.getPriority() : ticket.getPriority());

            slaInstanceService.createSlaInstance(ticket, newPolicy);

            // Re-evaluate Agent Assignment
            HDEmployee newAgent = ticketAssignmentService.assignAgent(ticket);
            if (newAgent != null && (ticket.getAssignedAgent() == null || !ticket.getAssignedAgent().getId().equals(newAgent.getId()))) {
                ticket.setAssignedAgent(newAgent);
                ticketHistoryService.log(ticket, current, TicketEventType.REASSIGNED, null, newAgent.getId().toString());
            }
        }

        HDTicket updated = ticketRepository.save(ticket);
        ticketHistoryService.log(ticket, current, TicketEventType.STATUS_CHANGED, null, "Ticket details updated");

        return TicketMapper.mapToResponse(updated, true);
    }


    // 4. DELETE TICKET (MANAGER / ADMIN)
    @Override
    public void deleteTicket(Long ticketId) {
        HDTicket ticket = getTicketEntity(ticketId);
        HDEmployee current = currentUserService.getCurrentEmployee();

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

    // 5. GET ALL TICKETS WITH PAGINATION (GET /api/tickets?limit=10&offset=0)
    @Override
    @Transactional(readOnly = true)
    public PageResponse<TicketResponse> getAllTicketsPaginated(int limit, long offset) {
        int safeLimit = (limit <= 0) ? 10 : Math.min(limit, 100);
        if (offset < 0) {
            throw new BadRequestException("Offset cannot be negative");
        }

        HDEmployee current = currentUserService.getCurrentEmployee();
        int pageIndex = (int) (offset / safeLimit);
        Pageable pageable = PageRequest.of(pageIndex, safeLimit, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<HDTicket> spec = (root, query, cb) -> {
            if (current.getRole() == EmployeeRole.ADMIN) {
                return cb.conjunction();
            } else if (current.getRole() == EmployeeRole.MANAGER) {
                if (current.getDepartment() != null) {
                    return cb.equal(root.get("department").get("id"), current.getDepartment().getId());
                }
                return cb.conjunction();
            } else if (current.getRole() == EmployeeRole.AGENT) {
                Predicate assignedToMe = cb.equal(root.get("assignedAgent").get("id"), current.getId());
                Predicate inMyDept = (current.getDepartment() != null)
                        ? cb.equal(root.get("department").get("id"), current.getDepartment().getId())
                        : cb.disjunction();
                return cb.or(assignedToMe, inMyDept);
            } else {
                return cb.equal(root.get("requester").get("id"), current.getId());
            }
        };

        Page<HDTicket> page = ticketRepository.findAll(spec, pageable);
        List<TicketResponse> content = page.getContent().stream()
                .map(t -> TicketMapper.mapToResponse(t, false))
                .toList();

        return PageResponse.<TicketResponse>builder()
                .content(content)
                .limit(safeLimit)
                .offset(offset)
                .totalElements(page.getTotalElements())
                .hasNext(page.hasNext())
                .build();
    }

    // 6. GET MY TICKETS (EMPLOYEE)
    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> getMyTickets(TicketStatus status) {
        Long employeeId = currentUserService.getEmployeeId();
        List<HDTicket> tickets = (status == null)
                ? ticketRepository.findByRequester_IdOrderByCreatedAtDesc(employeeId)
                : ticketRepository.findByRequester_IdAndStatusOrderByCreatedAtDesc(employeeId, status);

        return tickets.stream()
                .map(t -> TicketMapper.mapToResponse(t, false))
                .toList();
    }

    // 7. GET ASSIGNED TICKETS (AGENT / MANAGER)
    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> getAssignedTickets(TicketStatus status) {
        Long currentUserId = currentUserService.getEmployeeId();
        HDEmployee current = currentUserService.getCurrentEmployee();

        List<HDTicket> tickets;
        if (current.getRole() == EmployeeRole.MANAGER) {
            tickets = (status == null)
                    ? ticketRepository.findByAssignedManager_IdOrderByCreatedAtDesc(currentUserId)
                    : ticketRepository.findByAssignedManager_IdAndStatusOrderByCreatedAtDesc(currentUserId, status);
        } else {
            tickets = (status == null)
                    ? ticketRepository.findByAssignedAgent_IdOrderByCreatedAtDesc(currentUserId)
                    : ticketRepository.findByAssignedAgent_IdAndStatusOrderByCreatedAtDesc(currentUserId, status);
        }

        return tickets.stream().map(t -> TicketMapper.mapToResponse(t, false)).toList();
    }

    // 8. GET DEPARTMENT TICKETS (MANAGER)
    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> getDepartmentTickets(Long agentId, TicketStatus status) {
        HDEmployee current = currentUserService.getCurrentEmployee();
        if (current.getDepartment() == null) {
            throw new BadRequestException("Manager does not have an assigned department");
        }
        Long departmentId = current.getDepartment().getId();

        List<HDTicket> tickets;
        if (agentId != null && status != null) {
            tickets = ticketRepository.findByDepartment_IdAndAssignedAgent_IdAndStatusOrderByCreatedAtDesc(departmentId, agentId, status);
        } else if (agentId != null) {
            tickets = ticketRepository.findByDepartment_IdAndAssignedAgent_IdOrderByCreatedAtDesc(departmentId, agentId);
        } else if (status != null) {
            tickets = ticketRepository.findByDepartment_IdAndStatusOrderByCreatedAtDesc(departmentId, status);
        } else {
            tickets = ticketRepository.findByDepartment_IdOrderByCreatedAtDesc(departmentId);
        }

        return tickets.stream().map(t -> TicketMapper.mapToResponse(t, false)).toList();
    }

    // 9. WITHDRAW TICKET
    @Override
    public void withdrawTicket(Long ticketId, WithdrawTicketRequest request) {
        HDTicket ticket = getTicketEntity(ticketId);
        HDEmployee current = currentUserService.getCurrentEmployee();

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
    public TicketResponse resolveTicket(Long ticketId, ResolveTicketRequest request) {
        HDTicket ticket = getTicketEntity(ticketId);
        HDEmployee current = currentUserService.getCurrentEmployee();

        validateCanResolve(ticket, current);

        if (ticket.getStatus() == TicketStatus.RESOLVED || ticket.getStatus() == TicketStatus.WITHDRAWN) {
            throw new InvalidOperationException("Ticket is already " + ticket.getStatus());
        }

        TicketStatus oldStatus = ticket.getStatus();
        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setResolutionSummary(request.getResolutionSummary().trim());
        ticket.setResolvedAt(Instant.now());

        HDTicket saved = ticketRepository.save(ticket);
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
        return TicketMapper.mapToResponse(saved, true);
    }

    // 11. WAITING FOR EMPLOYEE
    @Override
    public TicketResponse waitingForEmployee(Long ticketId, WaitingForEmployeeRequest request) {
        HDTicket ticket = getTicketEntity(ticketId);
        HDEmployee current = currentUserService.getCurrentEmployee();

        validateCanManageAssignedTicket(ticket, current);

        if (ticket.getStatus() == TicketStatus.RESOLVED || ticket.getStatus() == TicketStatus.WITHDRAWN) {
            throw new InvalidOperationException("Cannot set waiting status on a closed ticket");
        }

        TicketStatus oldStatus = ticket.getStatus();
        ticket.setStatus(TicketStatus.WAITING_FOR_EMPLOYEE);
        ticket.setHoldReason(request.getReason());
        ticket.setHoldStartedAt(Instant.now());

        HDTicket saved = ticketRepository.save(ticket);

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

        return TicketMapper.mapToResponse(saved, true);
    }

    // 12. RESUME TICKET (AGENT / MANAGER)
    @Override
    public TicketResponse resumeTicket(Long ticketId) {
        HDTicket ticket = getTicketEntity(ticketId);
        HDEmployee current = currentUserService.getCurrentEmployee();

        validateCanManageAssignedTicket(ticket, current);

        if (ticket.getStatus() != TicketStatus.WAITING_FOR_EMPLOYEE) {
            throw new InvalidOperationException("Ticket is not currently in WAITING_FOR_EMPLOYEE status");
        }

        TicketStatus oldStatus = ticket.getStatus();
        ticket.setStatus(TicketStatus.IN_PROGRESS);
        ticket.setHoldReason(null);
        ticket.setHoldStartedAt(null);

        HDTicket saved = ticketRepository.save(ticket);

        ticketHistoryService.log(
                saved,
                current,
                TicketEventType.STATUS_CHANGED,
                oldStatus.name(),
                TicketStatus.IN_PROGRESS.name()
        );

        return TicketMapper.mapToResponse(saved, true);
    }

    // 13. REOPEN TICKET (EMPLOYEE - 50% SLA ALLOCATION)
    @Override
    public TicketResponse reopenTicket(Long ticketId, ReopenTicketRequest request) {
        HDTicket ticket = getTicketEntity(ticketId);
        HDEmployee current = currentUserService.getCurrentEmployee();

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

        HDTicket saved = ticketRepository.save(ticket);

        // Recalculate SLA with 50% allocation for reopen cycle
        slaInstanceService.reopenSlaInstance(saved, saved.getSlaPolicy());

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
        return TicketMapper.mapToResponse(saved, true);
    }

    // 14. SUBMIT FEEDBACK (EMPLOYEE)
    @Override
    public void submitFeedback(Long ticketId, FeedbackRequest request) {
        HDTicket ticket = getTicketEntity(ticketId);
        HDEmployee current = currentUserService.getCurrentEmployee();

        if (!ticket.getRequester().getId().equals(current.getId())) {
            throw new AccessDeniedException("Only the requester can submit feedback");
        }

        if (ticket.getStatus() != TicketStatus.RESOLVED) {
            throw new InvalidOperationException("Feedback can only be submitted for RESOLVED tickets");
        }

        if (feedbackRepository.existsByTicket_Id(ticketId)) {
            throw new InvalidOperationException("Feedback has already been submitted for this ticket");
        }

        HDTicketFeedback feedback = new HDTicketFeedback();
        feedback.setTicket(ticket);
        feedback.setSubmittedBy(current);
        feedback.setRating(request.getRating());

        feedbackRepository.save(feedback);
        log.info("Feedback rating {} submitted for ticket {}", request.getRating(), ticket.getTicketNumber());
    }

    // 15. SEND MESSAGE
    @Override
    public void sendMessage(Long ticketId, SendMessageRequest request) {
        HDTicket ticket = getTicketEntity(ticketId);
        HDEmployee current = currentUserService.getCurrentEmployee();

        validateCanViewTicket(ticket, current);
        ticketMessageService.sendMessage(ticket, current, request.getMessage());
    }


    // HELPER & SECURITY VALIDATION METHODS
    private HDTicket getTicketEntity(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with id: " + ticketId));
    }

    private void validateCanViewTicket(HDTicket ticket, HDEmployee current) {
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

    private void validateCanResolve(HDTicket ticket, HDEmployee current) {
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

    private void validateCanManageAssignedTicket(HDTicket ticket, HDEmployee current) {
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

    private HDSlaPolicy resolveSlaPolicy(Long departmentId, Long subCategoryId) {

        return slaPolicyRepository.findByDepartment_IdAndSubCategory_IdAndActiveTrue(departmentId, subCategoryId)
                .orElseThrow(() -> new InvalidOperationException("No active SLA policy configured for the selected department and sub-category"));
    }

    private synchronized String generateUniqueTicketNumber() {
        Pageable topOne = PageRequest.of(0, 1);
        List<HDTicket> lastList = ticketRepository.findLastTicketForUpdate(topOne);
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

}