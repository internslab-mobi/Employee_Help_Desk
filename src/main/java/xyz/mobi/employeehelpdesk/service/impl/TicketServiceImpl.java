package xyz.mobi.employeehelpdesk.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import xyz.mobi.employeehelpdesk.dto.ticket.HoldTicketRequestDto;
import xyz.mobi.employeehelpdesk.dto.ticket.ResolveTicketRequestDto;
import xyz.mobi.employeehelpdesk.dto.ticket.WithdrawRequestDto;
import xyz.mobi.employeehelpdesk.entity.*;
import xyz.mobi.employeehelpdesk.entity.enums.SlaStatus;
import xyz.mobi.employeehelpdesk.repository.*;
import xyz.mobi.employeehelpdesk.entity.enums.AttachmentType;
import xyz.mobi.employeehelpdesk.entity.enums.HistoryEventType;
import xyz.mobi.employeehelpdesk.entity.enums.NotificationType;
import xyz.mobi.employeehelpdesk.entity.enums.TicketStatus;
import xyz.mobi.employeehelpdesk.exception.BadRequestException;
import xyz.mobi.employeehelpdesk.exception.ResourceNotFoundException;
import xyz.mobi.employeehelpdesk.service.CurrentUserService;
import xyz.mobi.employeehelpdesk.service.NotificationService;
import xyz.mobi.employeehelpdesk.service.TicketRoutingService;
import xyz.mobi.employeehelpdesk.service.SlaService;
import xyz.mobi.employeehelpdesk.dto.ticket.CreateTicketRequest;
import xyz.mobi.employeehelpdesk.dto.ticket.TicketResponse;
import xyz.mobi.employeehelpdesk.mapper.TicketAttachmentMapper;
import xyz.mobi.employeehelpdesk.mapper.TicketMapper;
import xyz.mobi.employeehelpdesk.service.TicketService;
import xyz.mobi.employeehelpdesk.service.helperservice.TicketHistoryService;
import xyz.mobi.employeehelpdesk.validator.TicketAttachmentValidator;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final TicketAttachmentRepository ticketAttachmentRepository;

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final SlaInstanceRepository  slaInstanceRepository;

    private final TicketMapper ticketMapper;
    private final TicketAttachmentMapper ticketAttachmentMapper;

    private final TicketAttachmentValidator ticketAttachmentValidator;

    private final TicketRoutingService ticketRoutingService;

    private final SlaService slaService;

    private final ObjectMapper objectMapper;
    private final DepartmentAgentRepository departmentAgentRepository;
    private final TicketHistoryService ticketHistoryService;
    private final NotificationService notificationService;
    private final CurrentUserService currentUserService;


    @Override
    @Transactional
    public TicketResponse createTicket(
            CreateTicketRequest request,
            List<MultipartFile> attachments,
            Long requesterId
    ) throws IOException {

        // 1. Find requester
        Employee requester = employeeRepository.findById(requesterId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Requester not found: " + requesterId
                        )
                );

        // 2. Find and validate department
        Department department = departmentRepository
                .findById(request.getDepartmentId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Department not found: " + request.getDepartmentId()
                        )
                );

        if (!department.getIsActive()) {
            throw new BadRequestException("Department is inactive");
        }

        // 3. Find and validate category
        Category category = findAndValidateCategory(
                request,
                department
        );


        // 4. Find and validate subcategory
        SubCategory subCategory = findAndValidateSubCategory(
                request,
                category
        );


        // 5. Create Ticket using MapStruct
        Ticket ticket = ticketMapper.toEntity(request);

        // used toBuilder to build on same object
        ticket = ticket.toBuilder()
                .requester(requester)
                .department(department)
                .category(category)
                .subCategory(subCategory)
                .build();


        // 6. Priority comes from SubCategory
        ticket.setPriority(subCategory.getPriority());

        // 7. Initial ticket status
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setReopenCount(0);

        // 8. Save Ticket first
        ticket = ticketRepository.save(ticket);

        // 9. Generate ticket number using generated Ticket ID
        ticket.setTicketNumber(generate(ticket.getId()));

        // 11. Save optional attachments
        ticketAttachmentValidator.validate(attachments);

        saveAttachments(
                ticket,
                attachments,
                requester
        );

        // 12. Create history
        ticketHistoryService.record(ticket,HistoryEventType.TICKET_CREATED,null,TicketStatus.OPEN);

        ticketRoutingService.routeTicket(ticket);

        slaService.startSla(ticket);

        SlaInstance slaInstance = slaInstanceRepository.findLatestByTicketId(ticket.getId());

        // 13. Convert Entity → Response DTO
        return ticketMapper.toResponse(ticket,slaInstance);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Integer> getEmployeeTicketStatus() {

        Long employeeId = currentUserService.getCurrentEmployeeId();

        Employee employee = employeeRepository
                .findById(employeeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Employee not found: " + employeeId
                        )
                );

        Map<String, Integer> counts = new java.util.LinkedHashMap<>();
        for (TicketStatus ts : TicketStatus.values()) {
            counts.put(ts.name(), 0);
        }

        if (employee.getTicketStatusCounts() != null && !employee.getTicketStatusCounts().isBlank()) {
            try {
                Map<String, Integer> parsed = objectMapper.readValue(
                        employee.getTicketStatusCounts(),
                        new TypeReference<Map<String, Integer>>() {}
                );
                if (parsed != null) {
                    parsed.forEach((k, v) -> counts.put(k, v != null ? v : 0));
                }
            } catch (Exception e) {
                throw new BadRequestException(
                        "Invalid ticket status counts JSON"
                );
            }
        }
        return counts;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Integer> getAgentTicketStatus() {

        Long employeeId = currentUserService.getCurrentEmployeeId();

        List<DepartmentAgent> agents = departmentAgentRepository.findByEmployeeId(employeeId);
        if (agents.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No agent assignment found for employee: " + employeeId
            );
        }

        Map<String, Integer> counts = new java.util.LinkedHashMap<>();
        for (TicketStatus ts : TicketStatus.values()) {
            counts.put(ts.name(), 0);
        }

        for (DepartmentAgent agent : agents) {
            if (agent.getTicketStatusCounts() != null && !agent.getTicketStatusCounts().isBlank()) {
                try {
                    Map<String, Integer> parsed = objectMapper.readValue(
                            agent.getTicketStatusCounts(),
                            new TypeReference<Map<String, Integer>>() {}
                    );
                    if (parsed != null) {
                        parsed.forEach((k, v) -> counts.merge(k, v != null ? v : 0, Integer::sum));
                    }
                } catch (Exception e) {
                    throw new BadRequestException(
                            "Invalid ticket status counts JSON"
                    );
                }
            }
        }
        return counts;
    }



    @Override
    @Transactional(readOnly = true)
    public Page<TicketResponse> getMyTickets(
            TicketStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            String search,
            Pageable pageable
    ) {

        Long requesterId = currentUserService.getCurrentEmployeeId();

        Page<Ticket> tickets;

        LocalDateTime from = fromDate != null
                ? fromDate.atStartOfDay()
                : null;

        LocalDateTime to = toDate != null
                ? toDate.plusDays(1)
                .atStartOfDay()
                .minusNanos(1)
                : null;

        if (search != null && !search.isBlank()) {

            tickets = ticketRepository.searchMyTickets(
                    requesterId,
                    search.trim(),
                    pageable
            );

        } else if (status != null && from != null && to != null) {

            tickets = ticketRepository
                    .findByRequesterIdAndStatusAndCreatedAtBetween(
                            requesterId,
                            status,
                            from,
                            to,
                            pageable
                    );

        } else if (from != null && to != null) {

            tickets = ticketRepository
                    .findByRequesterIdAndCreatedAtBetween(
                            requesterId,
                            from,
                            to,
                            pageable
                    );

        } else if (status != null) {

            tickets = ticketRepository.findByRequesterIdAndStatus(
                    requesterId,
                    status,
                    pageable
            );

        } else {

            tickets = ticketRepository.findByRequesterId(
                    requesterId,
                    pageable
            );
        }

        return buildTicketResponse(tickets);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<TicketResponse> getAgentTickets(
            TicketStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            String search,
            Pageable pageable
    ) {

        Long employeeId = currentUserService.getCurrentEmployeeId();

        List<DepartmentAgent> agents = departmentAgentRepository.findByEmployeeId(employeeId);
        if (agents.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No agent assignment found for employee: " + employeeId
            );
        }

        List<Long> agentIds = agents.stream().map(DepartmentAgent::getId).toList();

        Page<Ticket> tickets;

        LocalDateTime from = fromDate != null
                ? fromDate.atStartOfDay()
                : null;

        LocalDateTime to = toDate != null
                ? toDate.plusDays(1)
                .atStartOfDay()
                .minusNanos(1)
                : null;

        if (search != null && !search.isBlank()) {

            tickets = ticketRepository.searchMyAssignedTickets(
                    agentIds,
                    search.trim(),
                    pageable
            );

        } else if (status != null && from != null && to != null) {

            tickets = ticketRepository
                    .findByAssignedAgentIdInAndStatusAndCreatedAtBetween(
                            agentIds,
                            status,
                            from,
                            to,
                            pageable
                    );

        } else if (from != null && to != null) {

            tickets = ticketRepository
                    .findByAssignedAgentIdInAndCreatedAtBetween(
                            agentIds,
                            from,
                            to,
                            pageable
                    );

        } else if (status != null) {

            tickets = ticketRepository
                    .findByAssignedAgentIdInAndStatus(
                            agentIds,
                            status,
                            pageable
                    );

        } else {

            tickets = ticketRepository.findByAssignedAgentIdIn(
                    agentIds,
                    pageable
            );
        }

        return buildTicketResponse(tickets);

    }

    @Override
    @Transactional
    public TicketResponse withdrawTicket(Long ticketId, WithdrawRequestDto withdrawRequest) {

        String reason = withdrawRequest.reason();

        long employeeId = currentUserService.getCurrentEmployeeId();

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Ticket not found: " + ticketId
                        )
                );

        // Verify that this ticket belongs to the employee
        if (!ticket.getRequester().getId().equals(employeeId)) {
            throw new AccessDeniedException(
                    "You are not allowed to withdraw this ticket"
            );
        }

        // Validate current ticket status
        if (ticket.getStatus() == TicketStatus.RESOLVED) {
            throw new BadRequestException(
                    "Resolved ticket cannot be withdrawn"
            );
        }

        if (ticket.getStatus() == TicketStatus.WITHDRAWN) {
            throw new BadRequestException(
                    "Ticket is already withdrawn"
            );
        }

        TicketStatus oldStatus = ticket.getStatus();

        // Change ticket status
        ticket.setStatus(TicketStatus.WITHDRAWN);

        if (reason == null || reason.isBlank()) {
            throw new BadRequestException("Withdrawal reason is required");
        }

        ticket.setWithdrawnAt(LocalDateTime.now());

        ticket.setWithdrawalReason(reason);

        ticketRepository.save(ticket);

        // Create history
        ticketHistoryService.record(
                ticket,
                HistoryEventType.WITHDRAWN,
                oldStatus,
                TicketStatus.WITHDRAWN);

        SlaInstance slaInstance = slaInstanceRepository.findLatestByTicketId(ticketId);

        if (slaInstance != null) {
            slaInstance.setStatus(SlaStatus.WITHDRAWN);
            slaInstanceRepository.save(slaInstance);
        }

        if (ticket.getAssignedAgent() != null && ticket.getAssignedAgent().getEmployee() != null) {
            String ticketNumber = ticket.getTicketNumber() != null ? ticket.getTicketNumber() : ("#" + ticket.getId());
            notificationService.sendNotification(
                    ticket.getAssignedAgent().getEmployee(),
                    ticket,
                    NotificationType.TICKET_WITHDRAWN,
                    "Ticket Withdrawn",
                    "Ticket " + ticketNumber + " has been withdrawn."
            );
        }

        return ticketMapper.toResponse(
                ticket,
                slaInstance
        );
    }

    @Override
    @Transactional
    public TicketResponse startTicket(Long ticketId) {

        long currentAgentId = currentUserService.getCurrentEmployeeId();

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));

        if (ticket.getAssignedAgent() == null) {
            throw new BadRequestException("Ticket is not assigned to any agent");
        }

        boolean isCurrentAgent = (ticket.getAssignedAgent().getEmployee() != null && ticket.getAssignedAgent().getEmployee().getId().equals(currentAgentId))
                || ticket.getAssignedAgent().getId().equals(currentAgentId);

        if (!isCurrentAgent) {
            throw new AccessDeniedException("You are not allowed to start this ticket");
        }

        if (ticket.getStatus() != TicketStatus.OPEN && ticket.getStatus() != TicketStatus.REOPENED) {
            throw new BadRequestException("Ticket is not in OPEN or REOPENED status");
        }

        TicketStatus oldStatus = ticket.getStatus();

        ticket.setStatus(TicketStatus.IN_PROGRESS);
        ticketRepository.save(ticket);

        ticketHistoryService.record(
                ticket,
                HistoryEventType.STATUS_CHANGED,
                oldStatus,
                TicketStatus.IN_PROGRESS
        );

        SlaInstance slaInstance = slaInstanceRepository.findLatestByTicketId(ticketId);

        return ticketMapper.toResponse(ticket, slaInstance);
    }

    @Override
    @Transactional
    public TicketResponse holdTicket(Long ticketId, HoldTicketRequestDto request) {
        long currentAgentId = currentUserService.getCurrentEmployeeId();

        String reason = request != null ? request.reason() : null;
        if (reason == null || reason.isBlank()) {
            throw new BadRequestException("Hold reason is required");
        }

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));

        if (ticket.getAssignedAgent() == null) {
            throw new BadRequestException("Ticket is not assigned to any agent");
        }

        boolean isCurrentAgent = (ticket.getAssignedAgent().getEmployee() != null && ticket.getAssignedAgent().getEmployee().getId().equals(currentAgentId))
                || ticket.getAssignedAgent().getId().equals(currentAgentId);

        if (!isCurrentAgent) {
            throw new AccessDeniedException("You are not allowed to hold this ticket");
        }

        if (ticket.getStatus() != TicketStatus.IN_PROGRESS) {
            throw new BadRequestException("Ticket is not in IN_PROGRESS status");
        }

        ticket.setStatus(TicketStatus.ON_HOLD);
        ticket.setHoldReason(reason);
        ticket.setHoldStartedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        ticketHistoryService.record(
                ticket,
                HistoryEventType.HOLD,
                TicketStatus.IN_PROGRESS,
                TicketStatus.ON_HOLD
        );

        SlaInstance slaInstance = slaService.pauseSla(ticket);

        String ticketNumber = ticket.getTicketNumber() != null ? ticket.getTicketNumber() : ("#" + ticket.getId());
        notificationService.sendNotification(
                ticket.getRequester(),
                ticket,
                NotificationType.TICKET_ON_HOLD,
                "Ticket On Hold",
                "Ticket " + ticketNumber + " has been put on hold."
        );

        return ticketMapper.toResponse(ticket, slaInstance);
    }

    @Override
    @Transactional
    public TicketResponse resumeTicket(Long ticketId) {
        long currentAgentId = currentUserService.getCurrentEmployeeId();

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));

        if (ticket.getAssignedAgent() == null) {
            throw new BadRequestException("Ticket is not assigned to any agent");
        }

        boolean isCurrentAgent = (ticket.getAssignedAgent().getEmployee() != null && ticket.getAssignedAgent().getEmployee().getId().equals(currentAgentId))
                || ticket.getAssignedAgent().getId().equals(currentAgentId);

        if (!isCurrentAgent) {
            throw new AccessDeniedException("You are not allowed to resume this ticket");
        }

        if (ticket.getStatus() != TicketStatus.ON_HOLD) {
            throw new BadRequestException("Ticket is not in ON_HOLD status");
        }

        ticket.setStatus(TicketStatus.IN_PROGRESS);
        ticket.setHoldReason(null);
        ticket.setHoldStartedAt(null);
        ticketRepository.save(ticket);

        ticketHistoryService.record(
                ticket,
                HistoryEventType.RESUMED,
                TicketStatus.ON_HOLD,
                TicketStatus.IN_PROGRESS
        );

        SlaInstance slaInstance = slaService.resumeSla(ticket);

        String ticketNumber = ticket.getTicketNumber() != null ? ticket.getTicketNumber() : ("#" + ticket.getId());
        notificationService.sendNotification(
                ticket.getRequester(),
                ticket,
                NotificationType.TICKET_RESUMED,
                "Ticket Resumed",
                "Ticket " + ticketNumber + " has been resumed."
        );

        return ticketMapper.toResponse(ticket, slaInstance);
    }

    @Override
    @Transactional
    public TicketResponse resolveTicket(Long ticketId, ResolveTicketRequestDto request) {
        long currentAgentId = currentUserService.getCurrentEmployeeId();

        String summary = request != null ? request.resolutionSummary() : null;
        if (summary == null || summary.isBlank()) {
            throw new BadRequestException("Resolution summary is required");
        }

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));

        if (ticket.getAssignedAgent() == null) {
            throw new BadRequestException("Ticket is not assigned to any agent");
        }

        boolean isCurrentAgent = (ticket.getAssignedAgent().getEmployee() != null && ticket.getAssignedAgent().getEmployee().getId().equals(currentAgentId))
                || ticket.getAssignedAgent().getId().equals(currentAgentId);

        if (!isCurrentAgent) {
            throw new AccessDeniedException("You are not allowed to resolve this ticket");
        }

        if (ticket.getStatus() != TicketStatus.IN_PROGRESS) {
            throw new BadRequestException("Ticket is not in IN_PROGRESS status");
        }

        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setResolutionSummary(summary);
        ticket.setResolvedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        ticketHistoryService.record(
                ticket,
                HistoryEventType.RESOLVED,
                TicketStatus.IN_PROGRESS,
                TicketStatus.RESOLVED
        );

        SlaInstance slaInstance = slaService.completeSla(ticket);

        String ticketNumber = ticket.getTicketNumber() != null ? ticket.getTicketNumber() : ("#" + ticket.getId());
        notificationService.sendNotification(
                ticket.getRequester(),
                ticket,
                NotificationType.TICKET_RESOLVED,
                "Ticket Resolved",
                "Ticket " + ticketNumber + " has been resolved."
        );

        return ticketMapper.toResponse(ticket, slaInstance);
    }

    // Category Validation

    private Category findAndValidateCategory(
            CreateTicketRequest request,
            Department department
    ) {

        if (request.getCategoryId() == null) {

            throw new BadRequestException(
                    "Category is required"
            );
        }


        Category category = categoryRepository
                .findById(request.getCategoryId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Category not found: "
                                        + request.getCategoryId()
                        )
                );


        if (!category.getIsActive()) {

            throw new BadRequestException(
                    "Category is inactive"
            );
        }


        // Category must belong to selected department

        if (!category.getDepartment().getId()
                .equals(department.getId())) {

            throw new BadRequestException(
                    "Category does not belong to selected department"
            );
        }

        return category;
    }

    // SubCategory Validation

    private SubCategory findAndValidateSubCategory(
            CreateTicketRequest request,
            Category category
    ) {

        if (request.getSubCategoryId() == null) {

            throw new BadRequestException(
                    "Subcategory is required"
            );
        }


        SubCategory subCategory = subCategoryRepository
                .findById(request.getSubCategoryId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Subcategory not found: "
                                        + request.getSubCategoryId()
                        )
                );


        if (!subCategory.getIsActive()) {

            throw new BadRequestException(
                    "Subcategory is inactive"
            );
        }


        // SubCategory must belong to selected Category

        if (!subCategory.getCategory().getId()
                .equals(category.getId())) {

            throw new BadRequestException(
                    "Subcategory does not belong to selected category"
            );
        }


        return subCategory;
    }

    // Save Attachments

    private void saveAttachments(
            Ticket ticket,
            List<MultipartFile> attachments,
            Employee uploader
    ) throws IOException {

        // Attachments are optional

        if (attachments == null || attachments.isEmpty()) {
            return;
        }

        for (MultipartFile file : attachments) {

            // Ignore empty files
            if (file.isEmpty()) {
                continue;
            }

            // Read file data
            byte[] fileData = file.getBytes();


            // MultipartFile → TicketAttachment
            TicketAttachment attachment =
                    ticketAttachmentMapper.toEntity(
                            file,
                            ticket,
                            uploader,
                            fileData,
                            AttachmentType.INITIAL_ATTACHMENT
                    );

            ticketAttachmentRepository.save(attachment);
        }
    }

    private String generate(Long ticketId) {

        return String.format(
                "HD-%d-%06d",
                java.time.Year.now().getValue(),
                ticketId
        );
    }

    private Page<TicketResponse> buildTicketResponse(Page<Ticket> tickets){
        // Get ticket IDs from current page
        List<Long> ticketIds = tickets.getContent()
                .stream()
                .map(Ticket::getId)
                .toList();

        // Get latest SLA for all tickets in one query
        List<SlaInstance> slaInstances = ticketIds.isEmpty()
                ? Collections.emptyList()
                : slaInstanceRepository.findLatestByTicketIds(ticketIds);

        // Ticket ID -> latest SLA
        Map<Long, SlaInstance> slaMap = slaInstances.stream()
                .collect(Collectors.toMap(
                        sla -> sla.getTicket().getId(),
                        Function.identity()
                ));

        // Build response
        return tickets.map(ticket -> {

            SlaInstance slaInstance = slaMap.get(ticket.getId());

            return ticketMapper.toResponse(
                    ticket,
                    slaInstance
            );
        });
    }
}