package com.example.helpdesk.service.impl;

import com.example.helpdesk.dto.request.AssignManagerRequestDTO;
import com.example.helpdesk.dto.request.AssignTicketRequestDTO;
import com.example.helpdesk.dto.request.CreateTicketRequestDTO;
import com.example.helpdesk.dto.request.HoldTicketRequestDTO;
import com.example.helpdesk.dto.request.ReopenTicketRequestDTO;
import com.example.helpdesk.dto.request.ResolveTicketRequestDTO;
import com.example.helpdesk.dto.request.TicketFeedbackRequestDTO;
import com.example.helpdesk.dto.request.TicketMessageRequestDTO;
import com.example.helpdesk.dto.request.UpdateTicketCategoryRequestDTO;
import com.example.helpdesk.dto.request.UpdateTicketPriorityRequestDTO;
import com.example.helpdesk.dto.request.UpdateTicketRequestDTO;
import com.example.helpdesk.dto.request.UpdateTicketStatusRequestDTO;
import com.example.helpdesk.dto.request.WithdrawTicketRequestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.helpdesk.dto.response.AssignmentProposalResponseDTO;
import com.example.helpdesk.dto.response.AssignedAgentResponseDTO;
import com.example.helpdesk.dto.response.AssignedManagerResponseDTO;
import com.example.helpdesk.dto.response.AssignmentResponseDTO;
import com.example.helpdesk.dto.response.CategoryResponseDTO;
import com.example.helpdesk.dto.response.DepartmentResponseDTO;
import com.example.helpdesk.dto.response.EmployeeResponseDTO;
import com.example.helpdesk.dto.response.SlaResponseDTO;
import com.example.helpdesk.dto.response.SubCategoryResponseDTO;
import com.example.helpdesk.dto.response.TicketAttachmentResponseDTO;
import com.example.helpdesk.dto.response.TicketDetailsResponseDTO;
import com.example.helpdesk.dto.response.TicketFeedbackResponseDTO;
import com.example.helpdesk.dto.response.TicketMessageResponseDTO;
import com.example.helpdesk.dto.response.TicketResponseDTO;
import com.example.helpdesk.entity.AgentSkill;
import com.example.helpdesk.exception.AuthorizationException;
import com.example.helpdesk.exception.AuthenticationException;
import com.example.helpdesk.exception.ResourceNotFoundException;
import com.example.helpdesk.entity.Category;
import com.example.helpdesk.entity.Department;
import com.example.helpdesk.entity.DepartmentAgent;
import com.example.helpdesk.entity.DepartmentManager;
import com.example.helpdesk.entity.Employee;
import com.example.helpdesk.entity.Skill;
import com.example.helpdesk.entity.SubCategory;
import com.example.helpdesk.entity.SubCategorySkill;
import com.example.helpdesk.entity.Ticket;
import com.example.helpdesk.entity.TicketAttachment;
import com.example.helpdesk.entity.TicketFeedback;
import com.example.helpdesk.entity.TicketMessage;
import com.example.helpdesk.entity.TicketSla;
import com.example.helpdesk.enums.NotificationType;
import com.example.helpdesk.enums.Priority;
import com.example.helpdesk.enums.SlaStatus;
import com.example.helpdesk.enums.TicketEventType;
import com.example.helpdesk.enums.TicketStatus;
import com.example.helpdesk.mapper.TicketMapper;
import com.example.helpdesk.repository.AgentSkillRepository;
import com.example.helpdesk.util.AuthenticatedEmployeeUtil;
import com.example.helpdesk.util.TimezoneUtil;
import com.example.helpdesk.repository.CategoryRepository;
import com.example.helpdesk.repository.DepartmentAgentRepository;
import com.example.helpdesk.repository.DepartmentManagerRepository;
import com.example.helpdesk.repository.DepartmentRepository;
import com.example.helpdesk.repository.EmployeeRepository;
import com.example.helpdesk.repository.SubCategoryRepository;
import com.example.helpdesk.repository.SubCategorySkillRepository;
import com.example.helpdesk.repository.TicketAttachmentRepository;
import com.example.helpdesk.repository.TicketFeedbackRepository;
import com.example.helpdesk.repository.TicketMessageRepository;
import com.example.helpdesk.repository.TicketRepository;
import com.example.helpdesk.repository.TicketSlaRepository;
import com.example.helpdesk.service.NotificationService;
import com.example.helpdesk.service.SlaService;
import com.example.helpdesk.service.TicketHistoryService;
import com.example.helpdesk.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
    private final DepartmentManagerRepository departmentManagerRepository;
    private final TicketSlaRepository ticketSlaRepository;
    private final SubCategorySkillRepository subCategorySkillRepository;
    private final AgentSkillRepository agentSkillRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final TicketAttachmentRepository ticketAttachmentRepository;
    private final TicketFeedbackRepository ticketFeedbackRepository;

    private final SlaService slaService;
    private final AuthenticatedEmployeeUtil authenticatedEmployeeUtil;
    private final TicketHistoryService ticketHistoryService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    private final AtomicInteger ticketSequence = new AtomicInteger(1);

    private String generateTicketNumber() {
        Instant now = Instant.now();
        String timestamp = java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                .withZone(java.time.ZoneOffset.UTC)
                .format(now);
        int sequence = ticketSequence.getAndIncrement();
        return "TKT-" + timestamp + "-" + String.format("%04d", sequence);
    }

    private Long getAuthenticatedEmployeeId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new AuthorizationException("User not authenticated");
        }
        try {
            return Long.parseLong(authentication.getPrincipal().toString());
        } catch (NumberFormatException e) {
            throw new AuthorizationException("Invalid employee ID in authentication context");
        }
    }

    private String getAuthenticatedRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            throw new AuthorizationException("User not authenticated");
        }
        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(auth -> auth.startsWith("ROLE_"))
                .map(auth -> auth.substring(5))
                .findFirst()
                .orElseThrow(() -> new AuthorizationException("No role found in authentication context"));
    }

    private void checkTicketAccessForStatusUpdate(Ticket ticket, Long employeeId, String role) {
        if ("ADMIN".equals(role)) {
            return;
        }

        if ("EMPLOYEE".equals(role)) {
            if (!ticket.getRequester().getId().equals(employeeId)) {
                throw new AuthorizationException("You can only update status for your own tickets");
            }
            return;
        }

        if ("AGENT".equals(role)) {
            if (ticket.getAssignedAgent() == null || !ticket.getAssignedAgent().getEmployee().getId().equals(employeeId)) {
                throw new AuthorizationException("You can only update status for tickets assigned to you");
            }
            return;
        }

        if ("MANAGER".equals(role)) {
            if (!departmentManagerRepository.existsByDepartmentIdAndEmployeeId(ticket.getDepartment().getId(), employeeId)) {
                throw new AuthorizationException("You can only update status for tickets in your managed department");
            }
            return;
        }

        throw new AuthorizationException("Unauthorized role");
    }

    private void checkTicketAccessForAgentOperations(Ticket ticket, Long employeeId, String role) {
        if ("ADMIN".equals(role)) {
            return;
        }

        if ("AGENT".equals(role)) {
            if (ticket.getAssignedAgent() == null || !ticket.getAssignedAgent().getEmployee().getId().equals(employeeId)) {
                throw new AuthorizationException("You can only perform this operation on tickets assigned to you");
            }
            return;
        }

        if ("MANAGER".equals(role)) {
            if (!departmentManagerRepository.existsByDepartmentIdAndEmployeeId(ticket.getDepartment().getId(), employeeId)) {
                throw new AuthorizationException("You can only perform this operation on tickets in your managed department");
            }
            return;
        }

        throw new AuthorizationException("Unauthorized role");
    }

    private void checkTicketAccessForManagerOperations(Ticket ticket, Long employeeId, String role) {
        if ("ADMIN".equals(role)) {
            return;
        }

        if ("MANAGER".equals(role)) {
            if (!departmentManagerRepository.existsByDepartmentIdAndEmployeeId(ticket.getDepartment().getId(), employeeId)) {
                throw new AuthorizationException("You can only perform this operation on tickets in your managed department");
            }
            return;
        }

        throw new AuthorizationException("Unauthorized role");
    }

    private void checkTicketAccessForReopen(Ticket ticket, Long employeeId, String role) {
        if ("ADMIN".equals(role)) {
            return;
        }

        if ("EMPLOYEE".equals(role)) {
            if (!ticket.getRequester().getId().equals(employeeId)) {
                throw new AuthorizationException("You can only reopen your own tickets");
            }
            return;
        }

        if ("AGENT".equals(role)) {
            if (ticket.getAssignedAgent() == null || !ticket.getAssignedAgent().getEmployee().getId().equals(employeeId)) {
                throw new AuthorizationException("You can only reopen tickets assigned to you");
            }
            return;
        }

        if ("MANAGER".equals(role)) {
            if (!departmentManagerRepository.existsByDepartmentIdAndEmployeeId(ticket.getDepartment().getId(), employeeId)) {
                throw new AuthorizationException("You can only reopen tickets in your managed department");
            }
            return;
        }

        throw new AuthorizationException("Unauthorized role");
    }

    @Override
    @Transactional
    public TicketResponseDTO createTicketWithAttachment(CreateTicketRequestDTO request, Long uploadedById, MultipartFile file) {
        TicketResponseDTO response = createTicket(request);

        if (file != null && !file.isEmpty()) {
            validateFile(file);
            Employee uploader = employeeRepository.findById(uploadedById)
                    .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + uploadedById));

            Ticket ticket = ticketRepository.findById(response.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + response.getId()));

            try {
                byte[] fileData = file.getBytes();
                TicketAttachment attachment = TicketAttachment.builder()
                        .ticket(ticket)
                        .uploadedBy(uploader)
                        .message(null)
                        .originalFilename(file.getOriginalFilename())
                        .mimeType(file.getContentType())
                        .fileSize((long) fileData.length)
                        .fileData(fileData)
                        .attachmentType("TICKET")
                        .build();

                ticketAttachmentRepository.save(attachment);

                ticketHistoryService.recordHistory(
                        ticket,
                        uploader,
                        TicketEventType.ATTACHMENT_ADDED,
                        null,
                        "Attachment added to ticket: " + file.getOriginalFilename(),
                        null
                );

                log.info("Attachment uploaded for ticket {} during creation by {}", ticket.getId(), uploadedById);
            } catch (IOException e) {
                throw new RuntimeException("Failed to read file data", e);
            }
        }

        return response;
    }

    @Override
    public TicketResponseDTO createTicket(CreateTicketRequestDTO request) {

        // ---------------------------------------------------------
        // 1. Find requester
        // ---------------------------------------------------------
        Employee requester = employeeRepository.findById(request.getRequesterId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Employee not found: " + request.getRequesterId()
                        )
                );


        // ---------------------------------------------------------
        // 2. Find department
        // ---------------------------------------------------------
        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Department not found: " + request.getDepartmentId()
                        )
                );


        // ---------------------------------------------------------
        // 3. Find category
        // Category is mandatory
        // ---------------------------------------------------------
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
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
                                new ResourceNotFoundException(
                                        "SubCategory not found: "
                                                + request.getSubCategoryId()
                                )
                        );


        // ---------------------------------------------------------
        // 5. Validate category belongs to department
        // ---------------------------------------------------------
        if (!category.getDepartment().getId().equals(department.getId())) {
            throw new IllegalArgumentException(
                    "Category does not belong to the selected department"
            );
        }


        // ---------------------------------------------------------
        // 6. Validate sub-category belongs to category
        // ---------------------------------------------------------
        if (!subCategory.getCategory().getId().equals(category.getId())) {
            throw new IllegalArgumentException(
                    "Sub-category does not belong to the selected category"
            );
        }


        // ---------------------------------------------------------
        // 7. Get priority from sub-category
        // ---------------------------------------------------------
        String priority = subCategory.getPriority();


        // ---------------------------------------------------------
        // 8. Generate ticket number
        // ---------------------------------------------------------
        String ticketNumber = generateTicketNumber();


        // ---------------------------------------------------------
        // 9. Create ticket
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
        // 10. Save ticket
        // ---------------------------------------------------------
        Ticket savedTicket = ticketRepository.save(ticket);


        // ---------------------------------------------------------
        // 11. Create ticket history
        // ---------------------------------------------------------
        ticketHistoryService.recordHistory(
                savedTicket,
                requester,
                TicketEventType.TICKET_CREATED,
                null,
                null,
                null
        );


        // ---------------------------------------------------------
        // 12. Perform automatic routing and assignment
        // ---------------------------------------------------------
        try {
            DepartmentAgent assignedAgent = performRoutingAndAssignment(savedTicket);
            
            if (assignedAgent != null) {
                // ---------------------------------------------------------
                // 13. Assign department manager
                // ---------------------------------------------------------
                assignDepartmentManager(savedTicket);
                
                // ---------------------------------------------------------
                // 14. Create SLA instance after assignment
                // ---------------------------------------------------------
                TicketSla slaInstance = slaService.createSlaInstance(savedTicket);
                
                if (slaInstance != null) {
                    ticketHistoryService.recordHistory(
                            savedTicket,
                            requester,
                            TicketEventType.SLA_STARTED,
                            null,
                            null,
                            null
                    );
                }
                
                // ---------------------------------------------------------
                // 15. Send notification to assigned agent
                // ---------------------------------------------------------
                notificationService.sendNotification(
                        assignedAgent.getEmployee(),
                        savedTicket,
                        NotificationType.TICKET_CREATED,
                        "New Ticket Created",
                        "Ticket " + savedTicket.getTicketNumber() + " has been created and assigned to you."
                );

                notificationService.sendNotification(
                        assignedAgent.getEmployee(),
                        savedTicket,
                        NotificationType.AGENT_ASSIGNED,
                        "Agent Assigned",
                        "You have been assigned to ticket " + savedTicket.getTicketNumber() + "."
                );
                
                ticketHistoryService.recordHistory(
                        savedTicket,
                        requester,
                        TicketEventType.ASSIGNMENT_CONFIRMED,
                        null,
                        String.valueOf(assignedAgent.getId()),
                        null
                );
            }
        } catch (Exception e) {
            log.warn("Automatic routing failed for ticket {}: {}. Ticket created but not assigned.", savedTicket.getId(), e.getMessage());
            ticketHistoryService.recordHistory(
                    savedTicket,
                    requester,
                    TicketEventType.ROUTING_PROPOSED,
                    null,
                    "FAILED",
                    null
            );
        }


        log.info(
                "Ticket created with automated workflow. ticketId={}, ticketNumber={}",
                savedTicket.getId(),
                savedTicket.getTicketNumber()
        );

        return buildNestedTicketResponseDTO(savedTicket, false);
    }



    @Override
    public TicketResponseDTO updateStatus(Long ticketId, UpdateTicketStatusRequestDTO request) {
        log.info("updateStatus called - ticketId={}, requestedStatus={}", ticketId, request.getStatus());

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Ticket not found: " + ticketId));

        log.info("Ticket found - currentStatus={}", ticket.getStatus());

        Long employeeId = getAuthenticatedEmployeeId();
        String role = getAuthenticatedRole();
        checkTicketAccessForStatusUpdate(ticket, employeeId, role);

        String newStatus = request.getStatus().name();

        log.info("Setting status from {} to {}", ticket.getStatus(), newStatus);
        ticket.setStatus(newStatus);

        // Employee communication required -> pause SLA
        if (TicketStatus.NEED_EMPLOYEE_COMMUNICATION.name().equals(newStatus)) {

            ticket.setHoldReason("Waiting for employee communication");
            ticket.setHoldStartedAt(Instant.now());

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

        log.info("Before save - status={}", ticket.getStatus());
        Ticket savedTicket = ticketRepository.save(ticket);
        log.info("After save - savedTicketId={}, status={}", savedTicket.getId(), savedTicket.getStatus());

        // Notify requester about status change
        notificationService.sendNotification(
                ticket.getRequester(),
                ticket,
                NotificationType.STATUS_CHANGED,
                "Ticket Status Changed",
                "Your ticket " + ticket.getTicketNumber() + " status has been updated to: " + newStatus
        );

        log.info(
                "Ticket status updated. ticketId={}, newStatus={}",
                ticketId,
                newStatus
        );

        return buildNestedTicketResponseDTO(savedTicket, true);
    }

    @Override
    public TicketResponseDTO updatePriority(Long ticketId, UpdateTicketPriorityRequestDTO request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        Long employeeId = getAuthenticatedEmployeeId();
        String role = getAuthenticatedRole();
        checkTicketAccessForAgentOperations(ticket, employeeId, role);

        String newPriority = request.getPriority().name();
        ticket.setPriority(newPriority);
        Ticket savedTicket = ticketRepository.save(ticket);

        // Notify assigned agent and manager about priority change
        if (savedTicket.getAssignedAgent() != null) {
            notificationService.sendNotification(
                    savedTicket.getAssignedAgent().getEmployee(),
                    savedTicket,
                    NotificationType.PRIORITY_CHANGED,
                    "Priority Changed",
                    "Ticket " + savedTicket.getTicketNumber() + " priority has been changed to: " + newPriority
            );
        }

        if (savedTicket.getAssignedManager() != null) {
            notificationService.sendNotification(
                    savedTicket.getAssignedManager().getEmployee(),
                    savedTicket,
                    NotificationType.PRIORITY_CHANGED,
                    "Priority Changed",
                    "Ticket " + savedTicket.getTicketNumber() + " priority has been changed to: " + newPriority
            );
        }

        log.info("Ticket priority updated. ticketId={}, newPriority={}", ticketId, request.getPriority());
        return buildNestedTicketResponseDTO(savedTicket, true);
    }

    @Override
    public TicketResponseDTO updateCategory(Long ticketId, UpdateTicketCategoryRequestDTO request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        Long employeeId = getAuthenticatedEmployeeId();
        String role = getAuthenticatedRole();
        checkTicketAccessForManagerOperations(ticket, employeeId, role);

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + request.getCategoryId()));

        SubCategory subCategory = subCategoryRepository.findById(request.getSubCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("SubCategory not found: " + request.getSubCategoryId()));

        ticket.setCategory(category);
        ticket.setSubCategory(subCategory);
        ticket.setPriority(subCategory.getPriority());

        Ticket savedTicket = ticketRepository.save(ticket);

        // Notify assigned agent and manager about category change
        if (savedTicket.getAssignedAgent() != null) {
            notificationService.sendNotification(
                    savedTicket.getAssignedAgent().getEmployee(),
                    savedTicket,
                    NotificationType.CATEGORY_CHANGED,
                    "Category Changed",
                    "Ticket " + savedTicket.getTicketNumber() + " category has been changed."
            );
        }

        if (savedTicket.getAssignedManager() != null) {
            notificationService.sendNotification(
                    savedTicket.getAssignedManager().getEmployee(),
                    savedTicket,
                    NotificationType.CATEGORY_CHANGED,
                    "Category Changed",
                    "Ticket " + savedTicket.getTicketNumber() + " category has been changed."
            );
        }

        log.info("Ticket category updated. ticketId={}, newCategoryId={}", ticketId, request.getCategoryId());
        return buildNestedTicketResponseDTO(savedTicket, true);
    }

    @Override
    public TicketResponseDTO updateTicket(Long ticketId, UpdateTicketRequestDTO request) {
        log.info("PATCH request received - ticketId={}, operation={}", ticketId, request.getOperation());
        log.info("Request data: {}", request.getData());

        if (request.getOperation() == null) {
            throw new IllegalArgumentException("Operation is required");
        }

        if (request.getData() == null) {
            throw new IllegalArgumentException("Data is required");
        }

        // Dispatch based on operation
        switch (request.getOperation()) {
            case STATUS:
                log.info("Dispatching to STATUS handler");
                return handleStatusUpdate(ticketId, request.getData());
            case PRIORITY:
                log.info("Dispatching to PRIORITY handler");
                return handlePriorityUpdate(ticketId, request.getData());
            case CATEGORY:
                log.info("Dispatching to CATEGORY handler");
                return handleCategoryUpdate(ticketId, request.getData());
            case ASSIGN_AGENT:
                log.info("Dispatching to ASSIGN_AGENT handler");
                return handleAgentAssignment(ticketId, request.getData());
            case ASSIGN_MANAGER:
                log.info("Dispatching to ASSIGN_MANAGER handler");
                return handleManagerAssignment(ticketId, request.getData());
            case HOLD:
                log.info("Dispatching to HOLD handler");
                return handleHold(ticketId, request.getData());
            case RESUME:
                log.info("Dispatching to RESUME handler");
                return resumeTicket(ticketId);
            case RESOLVE:
                log.info("Dispatching to RESOLVE handler");
                return handleResolve(ticketId, request.getData());
            case REOPEN:
                log.info("Dispatching to REOPEN handler");
                return reopenTicketWithSla(ticketId);
            case WITHDRAW:
                log.info("Dispatching to WITHDRAW handler");
                return handleWithdraw(ticketId, request.getData());
            default:
                throw new IllegalArgumentException("Unsupported operation: " + request.getOperation());
        }
    }

    private TicketResponseDTO handleStatusUpdate(Long ticketId, com.fasterxml.jackson.databind.JsonNode data) {
        try {
            log.info("handleStatusUpdate called - ticketId={}, data={}", ticketId, data);
            UpdateTicketStatusRequestDTO statusRequest = objectMapper.treeToValue(data, UpdateTicketStatusRequestDTO.class);
            log.info("Parsed UpdateTicketStatusRequestDTO - status={}", statusRequest.getStatus());
            if (statusRequest.getStatus() == null) {
                throw new IllegalArgumentException("Status is required for STATUS operation");
            }
            TicketResponseDTO response = updateStatus(ticketId, statusRequest);
            log.info("updateStatus completed successfully - ticketId={}, newStatus={}", ticketId, response.getStatus());
            return response;
        } catch (Exception e) {
            log.error("Error in handleStatusUpdate - ticketId={}, error={}", ticketId, e.getMessage(), e);
            throw new IllegalArgumentException("Invalid data for STATUS operation: " + e.getMessage());
        }
    }

    private TicketResponseDTO handlePriorityUpdate(Long ticketId, com.fasterxml.jackson.databind.JsonNode data) {
        try {
            UpdateTicketPriorityRequestDTO priorityRequest = objectMapper.treeToValue(data, UpdateTicketPriorityRequestDTO.class);
            if (priorityRequest.getPriority() == null) {
                throw new IllegalArgumentException("Priority is required for PRIORITY operation");
            }
            return updatePriority(ticketId, priorityRequest);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid data for PRIORITY operation: " + e.getMessage());
        }
    }

    private TicketResponseDTO handleCategoryUpdate(Long ticketId, com.fasterxml.jackson.databind.JsonNode data) {
        try {
            UpdateTicketCategoryRequestDTO categoryRequest = objectMapper.treeToValue(data, UpdateTicketCategoryRequestDTO.class);
            if (categoryRequest.getCategoryId() == null || categoryRequest.getSubCategoryId() == null) {
                throw new IllegalArgumentException("CategoryId and SubCategoryId are required for CATEGORY operation");
            }
            return updateCategory(ticketId, categoryRequest);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid data for CATEGORY operation: " + e.getMessage());
        }
    }

    private TicketResponseDTO handleAgentAssignment(Long ticketId, com.fasterxml.jackson.databind.JsonNode data) {
        try {
            AssignTicketRequestDTO assignRequest = objectMapper.treeToValue(data, AssignTicketRequestDTO.class);
            if (assignRequest.getAgentId() == null) {
                throw new IllegalArgumentException("AgentId is required for ASSIGN_AGENT operation");
            }
            Long employeeId = getAuthenticatedEmployeeId();
            assignRequest.setAssignedBy(employeeId);
            return assignTicket(ticketId, assignRequest);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid data for ASSIGN_AGENT operation: " + e.getMessage());
        }
    }

    private TicketResponseDTO handleManagerAssignment(Long ticketId, com.fasterxml.jackson.databind.JsonNode data) {
        try {
            AssignManagerRequestDTO managerRequest = objectMapper.treeToValue(data, AssignManagerRequestDTO.class);
            if (managerRequest.getAssignedManagerId() == null) {
                throw new IllegalArgumentException("AssignedManagerId is required for ASSIGN_MANAGER operation");
            }
            
            Ticket ticket = ticketRepository.findById(ticketId)
                    .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));
            
            Long employeeId = getAuthenticatedEmployeeId();
            String role = getAuthenticatedRole();
            checkTicketAccessForManagerOperations(ticket, employeeId, role);
            
            DepartmentManager departmentManager = departmentManagerRepository.findById(managerRequest.getAssignedManagerId())
                    .orElseThrow(() -> new IllegalArgumentException("DepartmentManager not found: " + managerRequest.getAssignedManagerId()));
            
            ticket.setAssignedManager(departmentManager);
            
            notificationService.sendNotification(
                    departmentManager.getEmployee(),
                    ticket,
                    NotificationType.MANAGER_ASSIGNED,
                    "Manager Assigned",
                    "You have been assigned as manager for ticket " + ticket.getTicketNumber() + "."
            );
            
            Ticket savedTicket = ticketRepository.save(ticket);
            return buildNestedTicketResponseDTO(savedTicket, true);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid data for ASSIGN_MANAGER operation: " + e.getMessage());
        }
    }

    private TicketResponseDTO handleHold(Long ticketId, com.fasterxml.jackson.databind.JsonNode data) {
        try {
            log.info("handleHold called - ticketId={}, data={}", ticketId, data);
            HoldTicketRequestDTO holdRequest = objectMapper.treeToValue(data, HoldTicketRequestDTO.class);
            log.info("Parsed HoldTicketRequestDTO - holdReason={}", holdRequest.getHoldReason());
            if (holdRequest.getHoldReason() == null || holdRequest.getHoldReason().isBlank()) {
                throw new IllegalArgumentException("HoldReason is required for HOLD operation");
            }
            TicketResponseDTO response = holdTicket(ticketId, holdRequest);
            log.info("holdTicket completed successfully - ticketId={}, newStatus={}", ticketId, response.getStatus());
            return response;
        } catch (Exception e) {
            log.error("Error in handleHold - ticketId={}, error={}", ticketId, e.getMessage(), e);
            throw new IllegalArgumentException("Invalid data for HOLD operation: " + e.getMessage());
        }
    }

    private TicketResponseDTO handleResolve(Long ticketId, com.fasterxml.jackson.databind.JsonNode data) {
        try {
            ResolveTicketRequestDTO resolveRequest = objectMapper.treeToValue(data, ResolveTicketRequestDTO.class);
            if (resolveRequest.getResolutionSummary() == null || resolveRequest.getResolutionSummary().isBlank()) {
                throw new IllegalArgumentException("ResolutionSummary is required for RESOLVE operation");
            }
            return resolveTicketWithSummary(ticketId, resolveRequest);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid data for RESOLVE operation: " + e.getMessage());
        }
    }

    private TicketResponseDTO handleWithdraw(Long ticketId, com.fasterxml.jackson.databind.JsonNode data) {
        try {
            WithdrawTicketRequestDTO withdrawRequest = objectMapper.treeToValue(data, WithdrawTicketRequestDTO.class);
            if (withdrawRequest.getWithdrawalReason() == null || withdrawRequest.getWithdrawalReason().isBlank()) {
                throw new IllegalArgumentException("WithdrawalReason is required for WITHDRAW operation");
            }
            return withdrawTicket(ticketId, withdrawRequest.getWithdrawalReason());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid data for WITHDRAW operation: " + e.getMessage());
        }
    }

    @Override
    public TicketResponseDTO assignTicket(Long ticketId, AssignTicketRequestDTO request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        DepartmentAgent departmentAgent = departmentAgentRepository.findByEmployeeId(request.getAgentId())
                .orElseThrow(() -> new IllegalArgumentException("DepartmentAgent not found for employee: " + request.getAgentId()));

        ticket.setAssignedAgent(departmentAgent);
        Ticket savedTicket = ticketRepository.save(ticket);

        // Notify newly assigned agent
        notificationService.sendNotification(
                departmentAgent.getEmployee(),
                savedTicket,
                NotificationType.TICKET_REASSIGNED,
                "Ticket Reassigned",
                "Ticket " + savedTicket.getTicketNumber() + " has been reassigned to you."
        );

        log.info("Ticket assigned. ticketId={}, agentId={}", ticketId, request.getAgentId());
        return buildNestedTicketResponseDTO(savedTicket, true);
    }

    @Override
    public TicketResponseDTO resolveTicket(
            Long ticketId,
            UpdateTicketStatusRequestDTO request) {

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Ticket not found: " + ticketId));

        ticket.setStatus(TicketStatus.RESOLVED.name());
        ticket.setResolvedAt(Instant.now());

        if (request.getResolutionSummary() != null
                && !request.getResolutionSummary().isBlank()) {

            ticket.setResolutionSummary(request.getResolutionSummary());
        }

        slaService.completeSla(ticketId);

        Ticket savedTicket = ticketRepository.save(ticket);

        log.info("Ticket resolved. ticketId={}", ticketId);

        return buildNestedTicketResponseDTO(savedTicket, true);
    }

    @Override
    public TicketResponseDTO reopenTicket(Long ticketId, ReopenTicketRequestDTO request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));
        ticket.setStatus(TicketStatus.REOPENED.name());
        ticket.setReopenedAt(Instant.now());
        Ticket savedTicket = ticketRepository.save(ticket);
        log.info("Ticket reopened. ticketId={}", ticketId);
        return buildNestedTicketResponseDTO(savedTicket, true);
    }

    @Override
    public TicketResponseDTO holdTicket(Long ticketId, HoldTicketRequestDTO request) {
        log.info("holdTicket called - ticketId={}, holdReason={}", ticketId, request.getHoldReason());
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        log.info("Ticket found - currentStatus={}, assignedAgent={}", ticket.getStatus(), 
            ticket.getAssignedAgent() != null ? ticket.getAssignedAgent().getId() : "null");

        Long employeeId = getAuthenticatedEmployeeId();
        String role = getAuthenticatedRole();
        checkTicketAccessForAgentOperations(ticket, employeeId, role);

        if (ticket.getAssignedAgent() == null) {
            throw new IllegalStateException("Ticket must have an assigned agent to be put on hold");
        }

        if (ticket.getHoldStartedAt() != null) {
            throw new IllegalStateException("Ticket is already on hold");
        }

        ticket.setHoldReason(request.getHoldReason());
        ticket.setHoldStartedAt(Instant.now());
        ticket.setStatus(TicketStatus.NEED_EMPLOYEE_COMMUNICATION.name());

        log.info("Before save - status={}, holdReason={}", ticket.getStatus(), ticket.getHoldReason());
        Ticket savedTicket = ticketRepository.save(ticket);
        log.info("After save - savedTicketId={}, status={}, holdReason={}", savedTicket.getId(), savedTicket.getStatus(), savedTicket.getHoldReason());

        slaService.pauseSla(ticketId);

        ticketHistoryService.recordHistory(
                ticket,
                ticket.getAssignedAgent().getEmployee(),
                TicketEventType.SLA_HOLD,
                null,
                "Ticket put on hold: " + request.getHoldReason(),
                null
        );

        notificationService.sendNotification(
                ticket.getRequester(),
                ticket,
                NotificationType.TICKET_HOLD,
                "Ticket On Hold",
                "Your ticket " + ticket.getTicketNumber() + " has been put on hold. Reason: " + request.getHoldReason()
        );

        // Notify assigned agent about hold
        if (ticket.getAssignedAgent() != null) {
            notificationService.sendNotification(
                    ticket.getAssignedAgent().getEmployee(),
                    ticket,
                    NotificationType.TICKET_HOLD,
                    "Ticket On Hold",
                    "Ticket " + ticket.getTicketNumber() + " has been put on hold. Reason: " + request.getHoldReason()
            );
        }

        log.info("Ticket {} put on hold by agent - final status={}", ticketId, savedTicket.getStatus());

        return buildNestedTicketResponseDTO(savedTicket, true);
    }

    @Override
    public TicketResponseDTO resumeTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        Long employeeId = getAuthenticatedEmployeeId();
        String role = getAuthenticatedRole();
        checkTicketAccessForAgentOperations(ticket, employeeId, role);

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

        // Notify requester about resume
        notificationService.sendNotification(
                ticket.getRequester(),
                ticket,
                NotificationType.TICKET_RESUMED,
                "Ticket Resumed",
                "Your ticket " + ticket.getTicketNumber() + " has been resumed."
        );

        log.info("Ticket {} resumed", ticketId);

        return buildNestedTicketResponseDTO(savedTicket, true);
    }

    @Override
    public TicketResponseDTO resolveTicketWithSummary(Long ticketId, ResolveTicketRequestDTO request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        Long employeeId = getAuthenticatedEmployeeId();
        String role = getAuthenticatedRole();
        checkTicketAccessForAgentOperations(ticket, employeeId, role);

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
        ticket.setResolvedAt(Instant.now());

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

        return buildNestedTicketResponseDTO(savedTicket, true);
    }

    @Override
    public TicketResponseDTO reopenTicketWithSla(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        Long employeeId = getAuthenticatedEmployeeId();
        String role = getAuthenticatedRole();
        checkTicketAccessForReopen(ticket, employeeId, role);

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

        ticket.setReopenCount(ticket.getReopenCount() + 1);
        ticket.setReopenedAt(Instant.now());
        ticket.setResolvedAt(null);
        ticket.setResolutionSummary(null);
        ticket.setStatus(TicketStatus.IN_PROGRESS.name());

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

        if (ticket.getAssignedManager() != null) {
            notificationService.sendNotification(
                    ticket.getAssignedManager().getEmployee(),
                    ticket,
                    NotificationType.TICKET_REOPENED,
                    "Ticket Reopened",
                    "Ticket " + ticket.getTicketNumber() + " has been reopened. New SLA cycle started."
            );
        }

        log.info("Ticket {} reopened with new SLA cycle, allocated minutes: {}", ticketId, newAllocatedMinutes);

        return buildNestedTicketResponseDTO(savedTicket, true);
    }

    @Override
    public TicketResponseDTO withdrawTicket(Long ticketId, String withdrawalReason) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        Long employeeId = getAuthenticatedEmployeeId();
        String role = getAuthenticatedRole();

        // Check authorization: only requester can withdraw their own ticket
        if (!"ADMIN".equals(role)) {
            if (!ticket.getRequester().getId().equals(employeeId)) {
                throw new AuthorizationException("You can only withdraw your own tickets");
            }
        }

        // Check if ticket can be withdrawn
        if (ticket.getStatus().equals(TicketStatus.WITHDRAWN.name()) || 
            ticket.getStatus().equals(TicketStatus.CANCELLED.name())) {
            throw new IllegalStateException("Ticket is already withdrawn or cancelled");
        }

        if (ticket.getStatus().equals(TicketStatus.CLOSED.name())) {
            throw new IllegalStateException("Cannot withdraw a closed ticket");
        }

        // Withdraw the ticket
        ticket.setStatus(TicketStatus.WITHDRAWN.name());
        ticket.setWithdrawalReason(withdrawalReason);
        ticket.setWithdrawnAt(Instant.now());

        // Stop SLA if exists
        TicketSla ticketSla = ticketSlaRepository.findByTicketId(ticketId);
        if (ticketSla != null && !ticketSla.getStatus().equals(SlaStatus.COMPLETED.name())) {
            ticketSla.setStatus(SlaStatus.CANCELLED.name());
            ticketSlaRepository.save(ticketSla);
        }

        Ticket savedTicket = ticketRepository.save(ticket);

        // Record history
        ticketHistoryService.recordHistory(
                ticket,
                ticket.getRequester(),
                TicketEventType.STATUS_CHANGED,
                ticket.getStatus(),
                TicketStatus.WITHDRAWN.name(),
                null
        );

        // Send notification
        notificationService.sendNotification(
                ticket.getRequester(),
                ticket,
                NotificationType.TICKET_WITHDRAWN,
                "Ticket Withdrawn",
                "Your ticket " + ticket.getTicketNumber() + " has been withdrawn."
        );

        // Notify assigned agent if exists
        if (ticket.getAssignedAgent() != null) {
            notificationService.sendNotification(
                    ticket.getAssignedAgent().getEmployee(),
                    ticket,
                    NotificationType.TICKET_WITHDRAWN,
                    "Ticket Withdrawn",
                    "Ticket " + ticket.getTicketNumber() + " has been withdrawn by the requester."
            );
        }

        // Notify manager if exists
        if (ticket.getAssignedManager() != null) {
            notificationService.sendNotification(
                    ticket.getAssignedManager().getEmployee(),
                    ticket,
                    NotificationType.TICKET_WITHDRAWN,
                    "Ticket Withdrawn",
                    "Ticket " + ticket.getTicketNumber() + " has been withdrawn by the requester."
            );
        }

        log.info("Ticket {} withdrawn by employee {}", ticketId, employeeId);

        return buildNestedTicketResponseDTO(savedTicket, true);
    }

    // ==================== INTERNAL ROUTING HELPER ====================

    private DepartmentAgent performRoutingAndAssignment(Ticket ticket) {
        List<SubCategorySkill> requiredSkills = subCategorySkillRepository.findBySubCategoryId(
                ticket.getSubCategory().getId()
        );

        if (requiredSkills.isEmpty()) {
            throw new IllegalStateException("No required skills configured for sub-category: " 
                    + ticket.getSubCategory().getId());
        }

        List<Long> requiredSkillIds = requiredSkills.stream()
                .map(skill -> skill.getSkill().getId())
                .collect(Collectors.toList());

        List<DepartmentAgent> departmentAgents = departmentAgentRepository.findByDepartmentId(
                ticket.getDepartment().getId()
        );

        if (departmentAgents.isEmpty()) {
            throw new IllegalStateException("No agents found in department: " + ticket.getDepartment().getId());
        }

        List<AgentScore> scoredAgents = new ArrayList<>();

        for (DepartmentAgent agent : departmentAgents) {
            List<AgentSkill> agentSkills = agentSkillRepository.findByAgentId(agent.getId());
            List<Long> agentSkillIds = agentSkills.stream()
                    .map(skill -> skill.getSkill().getId())
                    .collect(Collectors.toList());

            long matchedSkills = requiredSkillIds.stream()
                    .filter(agentSkillIds::contains)
                    .count();

            if (matchedSkills > 0) {
                double skillScore = (matchedSkills * 100.0) / requiredSkillIds.size();
                int workload = calculateWorkload(agent.getId());

                AgentScore agentScore = AgentScore.builder()
                        .agent(agent)
                        .matchedSkills(matchedSkills)
                        .totalRequiredSkills(requiredSkillIds.size())
                        .skillScore(skillScore)
                        .workload(workload)
                        .lastAssignedAt(agent.getLastAssignedAt())
                        .build();

                scoredAgents.add(agentScore);
            }
        }

        if (scoredAgents.isEmpty()) {
            throw new IllegalStateException("No suitable agents found with required skills");
        }

        AgentScore bestAgent = scoredAgents.stream()
                .sorted(Comparator
                        .comparing(AgentScore::getSkillScore).reversed()
                        .thenComparing(AgentScore::getWorkload)
                        .thenComparing(AgentScore::getLastAssignedAt, 
                                Comparator.nullsFirst(Comparator.naturalOrder())))
                .findFirst()
                .orElseThrow();

        // Assign the best agent
        ticket.setAssignedAgent(bestAgent.getAgent());
        bestAgent.getAgent().setLastAssignedAt(Instant.now());
        ticketRepository.save(ticket);
        departmentAgentRepository.save(bestAgent.getAgent());

        log.info("Automatically assigned ticket {} to agent {} (skill score: {}, workload: {})",
                ticket.getId(), bestAgent.getAgent().getId(), bestAgent.getSkillScore(), bestAgent.getWorkload());

        return bestAgent.getAgent();
    }

    private void assignDepartmentManager(Ticket ticket) {
        Long departmentId = ticket.getDepartment().getId();
        
        // Try to find primary manager first
        DepartmentManager manager = departmentManagerRepository
                .findByDepartmentIdAndPrimaryTrue(departmentId)
                .orElse(null);
        
        // If no primary manager, find any manager for the department
        if (manager == null) {
            List<DepartmentManager> managers = departmentManagerRepository
                    .findByDepartmentId(departmentId);
            
            if (managers.isEmpty()) {
                throw new IllegalStateException(
                        "No manager is available for department ID: " + departmentId
                );
            }
            
            manager = managers.get(0);
        }
        
        // Assign the manager to the ticket
        ticket.setAssignedManager(manager);
        ticketRepository.save(ticket);

        // Send notification to assigned manager
        notificationService.sendNotification(
                manager.getEmployee(),
                ticket,
                NotificationType.MANAGER_ASSIGNED,
                "Manager Assigned",
                "You have been assigned as manager for ticket " + ticket.getTicketNumber() + "."
        );

        log.info("Automatically assigned ticket {} to manager {}",
                ticket.getId(), manager.getId());
    }

    // ==================== ROUTING ====================

    @Override
    @Transactional
    public AssignmentProposalResponseDTO getAssignmentProposal(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        if (ticket.getAssignedAgent() != null) {
            throw new IllegalStateException("Ticket is already assigned to an agent");
        }

        List<SubCategorySkill> requiredSkills = subCategorySkillRepository.findBySubCategoryId(
                ticket.getSubCategory().getId()
        );

        if (requiredSkills.isEmpty()) {
            throw new IllegalStateException("No required skills configured for sub-category: " 
                    + ticket.getSubCategory().getId());
        }

        List<Long> requiredSkillIds = requiredSkills.stream()
                .map(skill -> skill.getSkill().getId())
                .collect(Collectors.toList());

        List<DepartmentAgent> departmentAgents = departmentAgentRepository.findByDepartmentId(
                ticket.getDepartment().getId()
        );

        if (departmentAgents.isEmpty()) {
            throw new IllegalStateException("No agents found in department: " + ticket.getDepartment().getId());
        }

        List<AgentScore> scoredAgents = new ArrayList<>();

        for (DepartmentAgent agent : departmentAgents) {
            List<AgentSkill> agentSkills = agentSkillRepository.findByAgentId(agent.getId());
            List<Long> agentSkillIds = agentSkills.stream()
                    .map(skill -> skill.getSkill().getId())
                    .collect(Collectors.toList());

            long matchedSkills = requiredSkillIds.stream()
                    .filter(agentSkillIds::contains)
                    .count();

            if (matchedSkills > 0) {
                double skillScore = (matchedSkills * 100.0) / requiredSkillIds.size();
                int workload = calculateWorkload(agent.getId());

                AgentScore agentScore = AgentScore.builder()
                        .agent(agent)
                        .matchedSkills(matchedSkills)
                        .totalRequiredSkills(requiredSkillIds.size())
                        .skillScore(skillScore)
                        .workload(workload)
                        .lastAssignedAt(agent.getLastAssignedAt())
                        .build();

                scoredAgents.add(agentScore);
            }
        }

        if (scoredAgents.isEmpty()) {
            throw new IllegalStateException("No suitable agents found with required skills");
        }

        AgentScore bestAgent = scoredAgents.stream()
                .sorted(Comparator
                        .comparing(AgentScore::getSkillScore).reversed()
                        .thenComparing(AgentScore::getWorkload)
                        .thenComparing(AgentScore::getLastAssignedAt, 
                                Comparator.nullsFirst(Comparator.naturalOrder())))
                .findFirst()
                .orElseThrow();

        List<String> matchedSkillNames = getSkillNames(bestAgent.getAgent().getId(), requiredSkillIds);
        List<String> requiredSkillNames = requiredSkills.stream()
                .map(skill -> skill.getSkill().getName())
                .collect(Collectors.toList());

        ticketHistoryService.recordHistory(
                ticket,
                null,
                TicketEventType.ROUTING_PROPOSED,
                null,
                "Agent " + bestAgent.getAgent().getEmployee().getId() + " proposed",
                Map.of("agentId", bestAgent.getAgent().getId(), "skillScore", bestAgent.getSkillScore())
        );

        return AssignmentProposalResponseDTO.builder()
                .ticketId(ticket.getId())
                .ticketNumber(ticket.getTicketNumber())
                .proposedAgentId(bestAgent.getAgent().getId())
                .employeeId(bestAgent.getAgent().getEmployee().getId())
                .employeeName(bestAgent.getAgent().getEmployee().getFirstName() + " " 
                        + bestAgent.getAgent().getEmployee().getLastName())
                .matchedSkills(matchedSkillNames)
                .requiredSkills(requiredSkillNames)
                .skillScore(bestAgent.getSkillScore())
                .currentWorkload(bestAgent.getWorkload())
                .status("PROPOSED")
                .build();
    }

    @Override
    @Transactional
    public void confirmAssignment(Long ticketId, Long agentId, Boolean confirmed) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        if (ticket.getAssignedAgent() != null) {
            throw new IllegalStateException("Ticket is already assigned");
        }

        DepartmentAgent agent = departmentAgentRepository.findById(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + agentId));

        if (!agent.getDepartment().getId().equals(ticket.getDepartment().getId())) {
            throw new IllegalStateException("Agent does not belong to the ticket's department");
        }

        if (confirmed) {
            List<SubCategorySkill> requiredSkills = subCategorySkillRepository.findBySubCategoryId(
                    ticket.getSubCategory().getId()
            );
            List<Long> requiredSkillIds = requiredSkills.stream()
                    .map(skill -> skill.getSkill().getId())
                    .collect(Collectors.toList());

            List<AgentSkill> agentSkills = agentSkillRepository.findByAgentId(agentId);
            List<Long> agentSkillIds = agentSkills.stream()
                    .map(skill -> skill.getSkill().getId())
                    .collect(Collectors.toList());

            long matchedSkills = requiredSkillIds.stream()
                    .filter(agentSkillIds::contains)
                    .count();

            if (matchedSkills == 0) {
                throw new IllegalStateException("Agent does not have required skills");
            }

            ticket.setAssignedAgent(agent);
            agent.setLastAssignedAt(Instant.now());
            ticketRepository.save(ticket);
            departmentAgentRepository.save(agent);

            ticketHistoryService.recordHistory(
                    ticket,
                    null,
                    TicketEventType.ASSIGNMENT_CONFIRMED,
                    null,
                    "Agent " + agentId + " confirmed assignment",
                    Map.of("agentId", agentId)
            );

            slaService.createSlaInstance(ticket);

            notificationService.sendNotification(
                    ticket.getRequester(),
                    ticket,
                    NotificationType.ASSIGNMENT_CONFIRMED,
                    "Ticket Assigned",
                    "Your ticket " + ticket.getTicketNumber() + " has been assigned to an agent."
            );

            log.info("Assignment confirmed for ticket {} by agent {}", ticketId, agentId);
        } else {
            ticketHistoryService.recordHistory(
                    ticket,
                    null,
                    TicketEventType.ASSIGNMENT_REJECTED,
                    null,
                    "Agent " + agentId + " rejected assignment",
                    Map.of("agentId", agentId)
            );

            log.info("Assignment rejected for ticket {} by agent {}", ticketId, agentId);
        }
    }

    // ==================== MESSAGES ====================

    @Override
    @Transactional
    public TicketMessageResponseDTO sendMessage(Long ticketId, TicketMessageRequestDTO request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        Employee sender = employeeRepository.findById(request.getSenderId())
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + request.getSenderId()));

        if (ticket.getAssignedAgent() == null) {
            throw new IllegalStateException("Ticket must have an assigned agent to send messages");
        }

        boolean isRequester = ticket.getRequester().getId().equals(sender.getId());
        boolean isAssignedAgent = ticket.getAssignedAgent().getEmployee().getId().equals(sender.getId());

        if (!isRequester && !isAssignedAgent) {
            throw new IllegalStateException("Sender must be the requester or assigned agent");
        }

        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be blank");
        }

        TicketMessage message = TicketMessage.builder()
                .ticket(ticket)
                .sender(sender)
                .content(request.getContent().trim())
                .seen(false)
                .build();

        message = ticketMessageRepository.save(message);

        ticketHistoryService.recordHistory(
                ticket,
                sender,
                TicketEventType.MESSAGE_SENT,
                null,
                "Message sent by " + sender.getFirstName() + " " + sender.getLastName(),
                null
        );

        Employee recipient = isRequester ? ticket.getAssignedAgent().getEmployee() : ticket.getRequester();
        notificationService.sendNotification(
                recipient,
                ticket,
                NotificationType.NEW_MESSAGE,
                "New Message",
                "You have a new message on ticket " + ticket.getTicketNumber()
        );

        log.info("Message sent for ticket {} by sender {}", ticketId, sender.getId());

        return toMessageResponse(message);
    }

    @Override
    @Transactional
    public TicketMessageResponseDTO sendMessageWithAttachment(Long ticketId, TicketMessageRequestDTO request, Long uploadedById, MultipartFile file) {
        TicketMessageResponseDTO response = sendMessage(ticketId, request);

        if (file != null && !file.isEmpty()) {
            validateFile(file);
            Employee uploader = employeeRepository.findById(uploadedById)
                    .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + uploadedById));

            Ticket ticket = ticketRepository.findById(ticketId)
                    .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

            TicketMessage message = ticketMessageRepository.findById(response.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Message not found: " + response.getId()));

            try {
                byte[] fileData = file.getBytes();
                TicketAttachment attachment = TicketAttachment.builder()
                        .ticket(ticket)
                        .uploadedBy(uploader)
                        .message(message)
                        .originalFilename(file.getOriginalFilename())
                        .mimeType(file.getContentType())
                        .fileSize((long) fileData.length)
                        .fileData(fileData)
                        .attachmentType("MESSAGE")
                        .build();

                ticketAttachmentRepository.save(attachment);

                ticketHistoryService.recordHistory(
                        ticket,
                        uploader,
                        TicketEventType.ATTACHMENT_ADDED,
                        null,
                        "Attachment added to message: " + file.getOriginalFilename(),
                        null
                );

                Employee recipient = ticket.getRequester().getId().equals(uploader.getId())
                        ? ticket.getAssignedAgent().getEmployee()
                        : ticket.getRequester();
                notificationService.sendNotification(
                        recipient,
                        ticket,
                        NotificationType.ATTACHMENT_ADDED,
                        "New Attachment",
                        "A new attachment was added to a message on ticket " + ticket.getTicketNumber()
                );

                log.info("Attachment uploaded for message {} on ticket {} by {}", message.getId(), ticketId, uploadedById);
            } catch (IOException e) {
                throw new RuntimeException("Failed to read file data", e);
            }
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketMessageResponseDTO> getTicketMessages(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        return ticketMessageRepository.findByTicketIdOrderByCreatedAtAsc(ticketId)
                .stream()
                .map(this::toMessageResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void markMessageAsSeen(Long messageId) {
        TicketMessage message = ticketMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));

        message.setSeen(true);
        ticketMessageRepository.save(message);

        log.info("Message {} marked as seen", messageId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketMessageResponseDTO> getUnreadMessages(Long ticketId, Long recipientId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        Employee recipient = employeeRepository.findById(recipientId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + recipientId));

        return ticketMessageRepository.findByTicketIdAndSeenFalseOrderByCreatedAtAsc(ticketId)
                .stream()
                .filter(msg -> !msg.getSender().getId().equals(recipientId))
                .map(this::toMessageResponse)
                .collect(Collectors.toList());
    }

    // ==================== ATTACHMENTS ====================

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final List<String> ALLOWED_MIME_TYPES = List.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    @Override
    @Transactional
    public TicketAttachmentResponseDTO uploadAttachment(Long ticketId, Long uploadedById, MultipartFile file, Long messageId, String attachmentType) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        Employee uploader = employeeRepository.findById(uploadedById)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + uploadedById));

        validateUploader(ticket, uploader);
        validateFile(file);

        TicketMessage message = null;
        if (messageId != null) {
            message = ticketMessageRepository.findById(messageId)
                    .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));
            if (!message.getTicket().getId().equals(ticketId)) {
                throw new IllegalArgumentException("Message does not belong to this ticket");
            }
        }

        try {
            byte[] fileData = file.getBytes();
            TicketAttachment attachment = TicketAttachment.builder()
                    .ticket(ticket)
                    .uploadedBy(uploader)
                    .message(message)
                    .originalFilename(file.getOriginalFilename())
                    .mimeType(file.getContentType())
                    .fileSize((long) fileData.length)
                    .fileData(fileData)
                    .attachmentType(attachmentType != null ? attachmentType : "GENERAL")
                    .build();

            attachment = ticketAttachmentRepository.save(attachment);

            ticketHistoryService.recordHistory(
                    ticket,
                    uploader,
                    TicketEventType.ATTACHMENT_ADDED,
                    null,
                    "Attachment added: " + file.getOriginalFilename(),
                    null
            );

            if (message != null) {
                Employee recipient = ticket.getRequester().getId().equals(uploader.getId())
                        ? ticket.getAssignedAgent().getEmployee()
                        : ticket.getRequester();
                notificationService.sendNotification(
                        recipient,
                        ticket,
                        NotificationType.ATTACHMENT_ADDED,
                        "New Attachment",
                        "A new attachment was added to a message on ticket " + ticket.getTicketNumber()
                );
            }

            log.info("Attachment uploaded for ticket {} by {}", ticketId, uploadedById);

            return toAttachmentResponse(attachment);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file data", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketAttachmentResponseDTO> getTicketAttachments(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        return ticketAttachmentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId)
                .stream()
                .map(this::toAttachmentResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadAttachment(Long attachmentId) {
        TicketAttachment attachment = ticketAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("Attachment not found: " + attachmentId));

        return new ByteArrayResource(
                attachment.getFileData(),
                attachment.getOriginalFilename()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public TicketAttachment getAttachmentById(Long attachmentId) {
        return ticketAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("Attachment not found: " + attachmentId));
    }

    @Override
    public void deleteAttachment(Long attachmentId, Long requesterId) {
        TicketAttachment attachment = ticketAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("Attachment not found: " + attachmentId));

        Employee requester = employeeRepository.findById(requesterId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + requesterId));

        if (!attachment.getUploadedBy().getId().equals(requesterId)) {
            throw new IllegalStateException("Only the uploader can delete an attachment");
        }

        ticketAttachmentRepository.delete(attachment);
        log.info("Attachment {} deleted by {}", attachmentId, requesterId);
    }

    @Override
    public TicketAttachmentResponseDTO uploadMessageAttachment(Long ticketId, Long messageId, Long uploadedById, MultipartFile file) {
        return uploadAttachment(ticketId, uploadedById, file, messageId, "MESSAGE");
    }

    // ==================== FEEDBACK ====================

    @Override
    @Transactional
    public TicketFeedbackResponseDTO submitFeedback(Long ticketId, TicketFeedbackRequestDTO request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        Employee submitter = employeeRepository.findById(request.getSubmittedBy())
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + request.getSubmittedBy()));

        if (!ticket.getRequester().getId().equals(submitter.getId())) {
            throw new IllegalStateException("Only the requester can submit feedback");
        }

        if (!ticket.getStatus().equals(TicketStatus.RESOLVED.name()) 
                && !ticket.getStatus().equals(TicketStatus.CLOSED.name())) {
            throw new IllegalStateException("Ticket must be resolved or closed to submit feedback");
        }

        if (ticketFeedbackRepository.findByTicketId(ticketId).isPresent()) {
            throw new IllegalStateException("Feedback already exists for this ticket");
        }

        if (request.getRating() < 1 || request.getRating() > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        TicketFeedback feedback = TicketFeedback.builder()
                .ticket(ticket)
                .submittedBy(submitter)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        feedback = ticketFeedbackRepository.save(feedback);

        ticketHistoryService.recordHistory(
                ticket,
                submitter,
                TicketEventType.FEEDBACK_SUBMITTED,
                null,
                "Feedback submitted with rating: " + request.getRating(),
                null
        );

        if (ticket.getAssignedAgent() != null) {
            notificationService.sendNotification(
                    ticket.getAssignedAgent().getEmployee(),
                    ticket,
                    NotificationType.FEEDBACK_SUBMITTED,
                    "Feedback Received",
                    "Your ticket " + ticket.getTicketNumber() + " received feedback with rating: " + request.getRating()
            );
        }

        log.info("Feedback submitted for ticket {} by {}", ticketId, submitter.getId());

        return toFeedbackResponse(feedback);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TicketFeedbackResponseDTO> getTicketFeedback(Long ticketId) {
        return ticketFeedbackRepository.findByTicketId(ticketId)
                .map(this::toFeedbackResponse);
    }

    // ==================== HELPER METHODS ====================

    private int calculateWorkload(Long agentId) {
        List<String> inactiveStatuses = List.of("CLOSED", "RESOLVED", "WITHDRAWN");
        return (int) ticketRepository.countByAssignedAgentIdAndStatusNotIn(agentId, inactiveStatuses);
    }

    private List<String> getSkillNames(Long agentId, List<Long> requiredSkillIds) {
        List<AgentSkill> agentSkills = agentSkillRepository.findByAgentId(agentId);
        return agentSkills.stream()
                .map(AgentSkill::getSkill)
                .filter(skill -> requiredSkillIds.contains(skill.getId()))
                .map(Skill::getName)
                .collect(Collectors.toList());
    }

    private TicketMessageResponseDTO toMessageResponse(TicketMessage message) {
        String timezone = authenticatedEmployeeUtil.getAuthenticatedEmployeeTimezone();
        List<TicketAttachmentResponseDTO> attachments = ticketAttachmentRepository.findByMessageIdOrderByCreatedAtAsc(message.getId())
                .stream()
                .map(this::toAttachmentResponse)
                .collect(Collectors.toList());

        return TicketMessageResponseDTO.builder()
                .id(message.getId())
                .ticketId(message.getTicket().getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getFirstName() + " " + message.getSender().getLastName())
                .content(message.getContent())
                .seen(message.getSeen())
                .createdAt(TimezoneUtil.toOffsetDateTime(message.getCreatedAt(), timezone))
                .attachments(attachments)
                .build();
    }

    private void validateUploader(Ticket ticket, Employee uploader) {
        boolean isRequester = ticket.getRequester().getId().equals(uploader.getId());
        boolean isAssignedAgent = ticket.getAssignedAgent() != null
                && ticket.getAssignedAgent().getEmployee().getId().equals(uploader.getId());

        if (!isRequester && !isAssignedAgent) {
            throw new IllegalStateException("Uploader must be the requester or assigned agent");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new AuthenticationException("ERR_013", "File cannot be empty");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.trim().isEmpty()) {
            throw new AuthenticationException("ERR_013", "Filename cannot be blank");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new AuthenticationException("ERR_013", "File size exceeds maximum limit of 10MB");
        }

        String mimeType = file.getContentType();
        if (mimeType == null || !ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new AuthenticationException("ERR_013", "File type not allowed: " + mimeType);
        }
    }


    private TicketAttachmentResponseDTO toAttachmentResponse(TicketAttachment attachment) {
        String timezone = authenticatedEmployeeUtil.getAuthenticatedEmployeeTimezone();
        return TicketAttachmentResponseDTO.builder()
                .id(attachment.getId())
                .ticketId(attachment.getTicket().getId())
                .uploadedById(attachment.getUploadedBy().getId())
                .uploadedByName(attachment.getUploadedBy().getFirstName() + " " + attachment.getUploadedBy().getLastName())
                .messageId(attachment.getMessage() != null ? attachment.getMessage().getId() : null)
                .originalFilename(attachment.getOriginalFilename())
                .mimeType(attachment.getMimeType())
                .fileSize(attachment.getFileSize())
                .attachmentType(attachment.getAttachmentType())
                .createdAt(TimezoneUtil.toOffsetDateTime(attachment.getCreatedAt(), timezone))
                .build();
    }

    private List<TicketAttachmentResponseDTO> getTicketAttachmentsForResponse(Long ticketId) {
        return ticketAttachmentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId)
                .stream()
                .map(this::toAttachmentResponse)
                .collect(Collectors.toList());
    }

    private TicketFeedbackResponseDTO toFeedbackResponse(TicketFeedback feedback) {
        String timezone = authenticatedEmployeeUtil.getAuthenticatedEmployeeTimezone();
        return TicketFeedbackResponseDTO.builder()
                .id(feedback.getId())
                .ticketId(feedback.getTicket().getId())
                .submittedById(feedback.getSubmittedBy().getId())
                .submittedByName(feedback.getSubmittedBy().getFirstName() + " " + feedback.getSubmittedBy().getLastName())
                .rating(feedback.getRating())
                .comment(feedback.getComment())
                .createdAt(TimezoneUtil.toOffsetDateTime(feedback.getCreatedAt(), timezone))
                .build();
    }

    private TicketResponseDTO buildNestedTicketResponseDTO(Ticket ticket, boolean includeUpdatedAt) {
        String timezone = authenticatedEmployeeUtil.getAuthenticatedEmployeeTimezone();

        // Build requester
        TicketResponseDTO.RequesterResponse requester = null;
        if (ticket.getRequester() != null) {
            String name = buildFullName(ticket.getRequester().getFirstName(), ticket.getRequester().getLastName());
            requester = TicketResponseDTO.RequesterResponse.builder()
                    .id(ticket.getRequester().getId())
                    .name(name)
                    .build();
        }

        // Build department
        TicketResponseDTO.DepartmentResponse department = null;
        if (ticket.getDepartment() != null) {
            department = TicketResponseDTO.DepartmentResponse.builder()
                    .id(ticket.getDepartment().getId())
                    .name(ticket.getDepartment().getName())
                    .build();
        }

        // Build category
        TicketResponseDTO.CategoryResponse category = null;
        if (ticket.getCategory() != null) {
            category = TicketResponseDTO.CategoryResponse.builder()
                    .id(ticket.getCategory().getId())
                    .name(ticket.getCategory().getName())
                    .build();
        }

        // Build subCategory as top-level field
        TicketResponseDTO.SubCategoryResponse subCategory = null;
        if (ticket.getSubCategory() != null) {
            subCategory = TicketResponseDTO.SubCategoryResponse.builder()
                    .id(ticket.getSubCategory().getId())
                    .name(ticket.getSubCategory().getName())
                    .build();
        }

        // Build assigned agent
        TicketResponseDTO.AssignedAgentResponse assignedAgent = null;
        if (ticket.getAssignedAgent() != null && ticket.getAssignedAgent().getEmployee() != null) {
            String agentName = buildFullName(ticket.getAssignedAgent().getEmployee().getFirstName(), 
                    ticket.getAssignedAgent().getEmployee().getLastName());
            assignedAgent = TicketResponseDTO.AssignedAgentResponse.builder()
                    .id(ticket.getAssignedAgent().getEmployee().getId())
                    .name(agentName)
                    .build();
        }

        // Get manager ID
        Long managerId = null;
        if (ticket.getAssignedManager() != null && ticket.getAssignedManager().getEmployee() != null) {
            managerId = ticket.getAssignedManager().getEmployee().getId();
        }

        // Get assignedAt timestamp from DepartmentAgent.lastAssignedAt
        OffsetDateTime assignedAt = null;
        if (ticket.getAssignedAgent() != null && ticket.getAssignedAgent().getLastAssignedAt() != null) {
            assignedAt = TimezoneUtil.toOffsetDateTime(ticket.getAssignedAgent().getLastAssignedAt(), timezone);
        }

        // Get SLA status
        String slaStatus = getSlaStatus(ticket.getId());

        // Build final flat response
        TicketResponseDTO response = TicketResponseDTO.builder()
                .id(ticket.getId())
                .ticketNumber(ticket.getTicketNumber())
                .requester(requester)
                .department(department)
                .category(category)
                .subCategory(subCategory)
                .subject(ticket.getSubject())
                .description(ticket.getDescription())
                .priority(Priority.valueOf(ticket.getPriority()))
                .status(TicketStatus.valueOf(ticket.getStatus()))
                .assignedAgent(assignedAgent)
                .managerId(managerId)
                .reopenCount(ticket.getReopenCount())
                .createdAt(TimezoneUtil.toOffsetDateTime(ticket.getCreatedAt(), timezone))
                .resolvedAt(ticket.getResolvedAt() != null ? TimezoneUtil.toOffsetDateTime(ticket.getResolvedAt(), timezone) : null)
                .reopenedAt(ticket.getReopenedAt() != null ? TimezoneUtil.toOffsetDateTime(ticket.getReopenedAt(), timezone) : null)
                .assignedAt(assignedAt)
                .slaStatus(slaStatus)
                .build();

        return response;
    }

    private String buildFullName(String firstName, String lastName) {
        StringBuilder name = new StringBuilder();
        if (firstName != null && !firstName.isBlank()) {
            name.append(firstName);
        }
        if (lastName != null && !lastName.isBlank()) {
            if (name.length() > 0) {
                name.append(" ");
            }
            name.append(lastName);
        }
        return name.length() > 0 ? name.toString() : null;
    }

    private String getSlaStatus(Long ticketId) {
        TicketSla sla = ticketSlaRepository.findByTicketId(ticketId);
        if (sla == null) {
            return null;
        }
        return sla.getStatus();
    }

    @lombok.Data
    @lombok.Builder
    private static class AgentScore {
        private DepartmentAgent agent;
        private long matchedSkills;
        private int totalRequiredSkills;
        private double skillScore;
        private int workload;
        private Instant lastAssignedAt;
    }

}




