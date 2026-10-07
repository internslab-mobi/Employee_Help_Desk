package xyz.mobi.employeehelpdesk.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import xyz.mobi.employeehelpdesk.dto.feedback.TicketFeedbackCreateResponseDTO;
import xyz.mobi.employeehelpdesk.dto.feedback.TicketFeedbackRequestDTO;
import xyz.mobi.employeehelpdesk.dto.feedback.TicketFeedbackResponseDTO;
import xyz.mobi.employeehelpdesk.dto.ticket.*;
import xyz.mobi.employeehelpdesk.entity.*;
import xyz.mobi.employeehelpdesk.entity.enums.*;
import xyz.mobi.employeehelpdesk.exception.BadRequestException;
import xyz.mobi.employeehelpdesk.exception.DuplicateResourceException;
import xyz.mobi.employeehelpdesk.exception.InvalidStateException;
import xyz.mobi.employeehelpdesk.exception.ResourceNotFoundException;
import xyz.mobi.employeehelpdesk.mapper.TicketAttachmentMapper;
import xyz.mobi.employeehelpdesk.mapper.TicketFeedbackMapper;
import xyz.mobi.employeehelpdesk.mapper.TicketMapper;
import xyz.mobi.employeehelpdesk.repository.*;
import xyz.mobi.employeehelpdesk.service.*;
import xyz.mobi.employeehelpdesk.service.helperservice.TicketHistoryService;
import xyz.mobi.employeehelpdesk.util.InstantDateRange;
import xyz.mobi.employeehelpdesk.validator.TicketAttachmentValidator;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static xyz.mobi.employeehelpdesk.entity.enums.TicketStatus.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final TicketAttachmentRepository ticketAttachmentRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final SlaInstanceRepository slaInstanceRepository;
    private final TicketMapper ticketMapper;
    private final TicketAttachmentMapper ticketAttachmentMapper;
    private final TicketAttachmentValidator ticketAttachmentValidator;
    private final TicketRoutingService ticketRoutingService;
    private final SlaService slaService;
    private final ObjectMapper objectMapper;
    private final DepartmentAgentRepository departmentAgentRepository;
    private final TicketHistoryService ticketHistoryService;
    private final NotificationService notificationService;
    private final AuthService authService;
    private final TicketFeedbackRepository ticketFeedbackRepository;
    private final TicketFeedbackMapper ticketFeedbackMapper;
    private final SubCategorySkillRepository subCategorySkillRepository;
    private final AgentSkillRepository agentSkillRepository;
    private final DepartmentManagerRepository departmentManagerRepository;


    @Override
    @Transactional
    public TicketCreateResponseDTO createTicket(
            CreateTicketRequestDTO request,
            List<MultipartFile> attachments
    ) throws IOException {

        Long requesterId = authService.getCurrentEmployeeId();

        // Find requester
        Employee requester = employeeRepository.findById(requesterId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Requester not found: " + requesterId
                        )
                );

        // Find and validate department
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

        // Find and validate category
        Category category = findAndValidateCategory(
                request,
                department
        );


        // Find and validate subcategory
        SubCategory subCategory = findAndValidateSubCategory(
                request,
                category
        );


        // Create Ticket using MapStruct
        Ticket ticket = ticketMapper.toEntity(request);

        // used toBuilder to build on same object
        ticket = ticket.toBuilder()
                .requester(requester)
                .department(department)
                .category(category)
                .subCategory(subCategory)
                .priority(subCategory.getPriority())
                .status(OPEN)
                .reopenCount(0)
                .build();

        // Save Ticket first
        ticket = ticketRepository.save(ticket);

        // Generate ticket number using generated Ticket ID
        ticket.setTicketNumber(generate(ticket.getId()));

        // Save optional attachments
        ticketAttachmentValidator.validate(attachments);

        saveAttachments(
                ticket,
                attachments,
                requester
        );

        // Create history
        ticketHistoryService.record(ticket, HistoryEventType.TICKET_CREATED, null, TicketStatus.OPEN);

        ticketRoutingService.routeTicket(ticket);

        SlaInstance slaInstance = slaService.startSla(ticket);

        log.info("Ticket created successfully: ticketId={}, ticketNumber={}, requesterId={}, departmentId={}, categoryId={}, subCategoryId={}",
                ticket.getId(), ticket.getTicketNumber(), requesterId, department.getId(), category.getId(), subCategory.getId());

        // Convert Entity → Response DTO
        return ticketMapper.toCreateResponse(ticket, slaInstance);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponseDTO getTicket(Long ticketId) {

        Long currentEmployeeId = authService.getCurrentEmployeeId();

        Employee currentEmployee = employeeRepository
                .findById(currentEmployeeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Employee not found: " + currentEmployeeId
                        )
                );

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Ticket not found: " + ticketId
                        )
                );

        // 1. Admin
        if (currentEmployee.getRole() == UserRole.ADMIN) {
            return buildTicketResponse(ticket);
        }

        // 2. Ticket requester
        if (ticket.getRequester() != null && ticket.getRequester().getId().equals(currentEmployeeId)) {
            return buildTicketResponse(ticket);
        }

        // 3. Assigned agent
        if (ticket.getAssignedAgent() != null
                && ticket.getAssignedAgent().getEmployee() != null
                && ticket.getAssignedAgent().getEmployee().getId().equals(currentEmployeeId)) {
            return buildTicketResponse(ticket);
        }

        // 4. Manager of the relevant department
        if (currentEmployee.getRole() == UserRole.MANAGER && currentEmployee.getDepartment() != null) {
            Long managerDeptId = currentEmployee.getDepartment().getId();

            if (ticket.getRequester() != null
                    && ticket.getRequester().getDepartment() != null
                    && managerDeptId.equals(ticket.getRequester().getDepartment().getId())) {
                return buildTicketResponse(ticket);
            }

            if (ticket.getAssignedAgent() != null
                    && ticket.getAssignedAgent().getEmployee() != null
                    && ticket.getAssignedAgent().getEmployee().getDepartment() != null
                    && managerDeptId.equals(ticket.getAssignedAgent().getEmployee().getDepartment().getId())) {
                return buildTicketResponse(ticket);
            }

            if (ticket.getDepartment() != null
                    && managerDeptId.equals(ticket.getDepartment().getId())) {
                return buildTicketResponse(ticket);
            }
        }

        throw new AccessDeniedException(
                "You are not authorized to view this ticket"
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TicketResponseDTO> getAllTickets(
            TicketView view,
            Long employeeId,
            Pageable pageable
    ) {
        if (view == null) {
            throw new BadRequestException("TicketView is required");
        }

        Long currentEmployeeId = authService.getCurrentEmployeeId();
        Employee currentEmployee = employeeRepository
                .findById(currentEmployeeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Employee not found: " + currentEmployeeId
                        )
                );

        Page<Ticket> tickets = switch (view) {
            case CREATED -> {
                Employee targetEmployee = resolveCreatedTargetEmployee(employeeId, currentEmployee);
                yield ticketRepository.findByRequesterId(targetEmployee.getId(), pageable);
            }
            case ASSIGNED -> {
                DepartmentAgent agent = resolveAssignedTargetAgent(employeeId, currentEmployee);
                yield ticketRepository.findByAssignedAgentId(agent.getId(), pageable);
            }
            case DEPARTMENT -> {
                Long departmentId = resolveDepartmentForManager(employeeId, currentEmployee);
                yield departmentId != null
                        ? ticketRepository.findByDepartmentId(departmentId, pageable)
                        : ticketRepository.findAllTickets(pageable);
            }
        };

        return buildTicketResponse(tickets);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TicketResponseDTO> searchTickets(
            TicketView view,
            Long employeeId,
            TicketStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            String search,
            Boolean unassigned,
            SlaStatus slaStatus,
            Pageable pageable
    ) {
        if (view == null) {
            throw new BadRequestException("TicketView is required");
        }

        Long currentEmployeeId = authService.getCurrentEmployeeId();
        Employee currentEmployee = employeeRepository
                .findById(currentEmployeeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Employee not found: " + currentEmployeeId
                        )
                );

        InstantDateRange dateRange = InstantDateRange.of(
                fromDate,
                toDate,
                authService.getCurrentUserTimezone()
        );
        String trimmedSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        Page<Ticket> tickets = switch (view) {
            case CREATED -> {
                Employee targetEmployee = resolveCreatedTargetEmployee(employeeId, currentEmployee);
                yield ticketRepository.findCreatedTicketsWithFilters(
                        targetEmployee.getId(),
                        status,
                        dateRange.from(),
                        dateRange.toExclusive(),
                        trimmedSearch,
                        unassigned,
                        slaStatus,
                        pageable
                );
            }
            case ASSIGNED -> {
                DepartmentAgent agent = resolveAssignedTargetAgent(employeeId, currentEmployee);
                yield ticketRepository.findAssignedTicketsWithFilters(
                        agent.getId(),
                        status,
                        dateRange.from(),
                        dateRange.toExclusive(),
                        trimmedSearch,
                        unassigned,
                        slaStatus,
                        pageable
                );
            }
            case DEPARTMENT -> {
                Long departmentId = resolveDepartmentForManager(employeeId, currentEmployee);
                yield ticketRepository.findDepartmentTicketsWithFilters(
                        departmentId,
                        status,
                        dateRange.from(),
                        dateRange.toExclusive(),
                        trimmedSearch,
                        unassigned,
                        slaStatus,
                        pageable
                );
            }
        };

        return buildTicketResponse(tickets);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignableAgentResponseDTO> getAssignableAgents(Long ticketId) {
        Long currentEmployeeId = authService.getCurrentEmployeeId();
        Employee currentEmployee = employeeRepository.findById(currentEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + currentEmployeeId));

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));

        if (ticket.getDepartment() == null) {
            throw new BadRequestException("Ticket is not associated with any department");
        }

        Long departmentId = ticket.getDepartment().getId();

        if (currentEmployee.getRole() != UserRole.MANAGER && currentEmployee.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("Only managers and admins can view assignable agents");
        }

        if (currentEmployee.getRole() == UserRole.MANAGER) {
            if (currentEmployee.getDepartment() == null
                    || !departmentId.equals(currentEmployee.getDepartment().getId())) {
                throw new AccessDeniedException("You are not authorized to view assignable agents for this ticket");
            }
        }

        // 1. Fetch eligible agents in the ticket's department (ACTIVE status and AGENT role)
        List<DepartmentAgent> eligibleAgents = departmentAgentRepository.findEligibleAgents(departmentId);
        if (eligibleAgents.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> agentIds = eligibleAgents.stream()
                .map(DepartmentAgent::getId)
                .toList();

        // 2. Fetch required skills for the ticket subcategory
        Set<Long> requiredSkillIds = Collections.emptySet();
        if (ticket.getSubCategory() != null) {
            List<SubCategorySkill> requiredSkills =
                    subCategorySkillRepository.findBySubCategoryId(ticket.getSubCategory().getId());
            requiredSkillIds = requiredSkills.stream()
                    .map(scs -> scs.getSkill().getId())
                    .collect(Collectors.toSet());
        }
        int requiredSkillCount = requiredSkillIds.size();

        // 3. Batch fetch agent skills
        List<AgentSkill> agentSkills = agentSkillRepository.findByAgentIdIn(agentIds);
        Map<Long, Set<Long>> agentSkillMap = agentSkills.stream()
                .collect(Collectors.groupingBy(
                        (AgentSkill as) -> as.getAgent().getId(),
                        Collectors.mapping((AgentSkill as) -> as.getSkill().getId(), Collectors.toSet())
                ));

        // 4. Batch count active tickets
        Collection<TicketStatus> activeStatuses = List.of(
                TicketStatus.OPEN,
                TicketStatus.IN_PROGRESS,
                TicketStatus.ON_HOLD,
                TicketStatus.REOPENED
        );
        List<Object[]> countRows = ticketRepository.countActiveTicketsForAgents(agentIds, activeStatuses);
        Map<Long, Long> activeCountMap = countRows.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        // 5. Build responses
        final Set<Long> finalRequiredSkillIds = requiredSkillIds;
        List<AssignableAgentResponseDTO> responses = new java.util.ArrayList<>(eligibleAgents.stream()
                .map(agent -> {
                    Set<Long> skills = agentSkillMap.getOrDefault(agent.getId(), Collections.emptySet());
                    int matchCount = 0;
                    if (!finalRequiredSkillIds.isEmpty()) {
                        for (Long reqId : finalRequiredSkillIds) {
                            if (skills.contains(reqId)) {
                                matchCount++;
                            }
                        }
                    }
                    double percentage = requiredSkillCount > 0
                            ? ((double) matchCount / requiredSkillCount) * 100.0
                            : 0.0;
                    long activeTicketCount = activeCountMap.getOrDefault(agent.getId(), 0L);

                    Employee emp = agent.getEmployee();
                    return AssignableAgentResponseDTO.builder()
                            .agentId(agent.getId())
                            .employeeId(emp != null ? emp.getId() : null)
                            .employeeCode(emp != null ? emp.getEmployeeCode() : null)
                            .firstName(emp != null ? emp.getFirstName() : null)
                            .lastName(emp != null ? emp.getLastName() : null)
                            .email(emp != null ? emp.getEmail() : null)
                            .departmentId(emp != null && emp.getDepartment() != null ? emp.getDepartment().getId() : null)
                            .departmentName(emp != null && emp.getDepartment() != null ? emp.getDepartment().getName() : null)
                            .matchedSkillCount(matchCount)
                            .requiredSkillCount(requiredSkillCount)
                            .matchPercentage(percentage)
                            .activeTicketCount(activeTicketCount)
                            .lastAssignedAt(agent.getLastAssignedAt())
                            .build();
                })
                .toList());

        // Sort responses to put best candidates first
        responses.sort(
                Comparator
                        // 1. Higher skill percentage first
                        .comparingDouble(AssignableAgentResponseDTO::getMatchPercentage)
                        .reversed()
                        // 2. Lower active workload first
                        .thenComparingLong(AssignableAgentResponseDTO::getActiveTicketCount)
                        // 3. Longest time since assignment (null first)
                        .thenComparing(
                                AssignableAgentResponseDTO::getLastAssignedAt,
                                Comparator.nullsFirst(Comparator.naturalOrder())
                        )
                        // 4. Lower agent ID
                        .thenComparing(AssignableAgentResponseDTO::getAgentId)
        );

        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Integer> getTicketSummary(TicketView view, Long employeeId, Long departmentId) {
        if (view == null) {
            throw new BadRequestException("TicketView is required");
        }

        Long currentEmployeeId = authService.getCurrentEmployeeId();
        Employee currentEmployee = employeeRepository
                .findById(currentEmployeeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Employee not found: " + currentEmployeeId
                        )
                );

        Map<String, Integer> counts = new LinkedHashMap<>();
        for (TicketStatus status : TicketStatus.values()) {
            counts.put(status.name(), 0);
        }

        if (view == TicketView.DEPARTMENT) {
            Long targetDepartmentId = resolveDepartmentSummaryTarget(departmentId, currentEmployee);
            List<Object[]> rows = ticketRepository.countTicketsByStatusForDepartment(targetDepartmentId);

            for (Object[] row : rows) {
                TicketStatus status = (TicketStatus) row[0];
                Long count = (Long) row[1];
                if (status != null && count != null) {
                    counts.put(status.name(), count.intValue());
                }
            }
            return counts;
        }

        String jsonCounts = switch (view) {
            case CREATED -> {
                Employee targetEmployee = resolveCreatedTargetEmployee(employeeId, currentEmployee);
                yield targetEmployee.getTicketStatusCounts();
            }
            case ASSIGNED -> {
                DepartmentAgent agent = resolveAssignedTargetAgent(employeeId, currentEmployee);
                yield agent.getTicketStatusCounts();
            }
            case DEPARTMENT -> null;
        };

        if (jsonCounts != null && !jsonCounts.isBlank()) {
            try {
                Map<String, Integer> parsed = objectMapper.readValue(
                        jsonCounts,
                        new TypeReference<Map<String, Integer>>() {}
                );

                if (parsed != null) {
                    parsed.forEach(
                            (key, value) ->
                                    counts.put(key, value != null ? value : 0)
                    );
                }
            } catch (Exception e) {
                throw new BadRequestException(
                        "Invalid ticket status counts JSON"
                );
            }
        }

        return counts;
    }

    private Long resolveDepartmentSummaryTarget(Long departmentId, Employee currentEmployee) {
        if (currentEmployee.getRole() != UserRole.MANAGER && currentEmployee.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("Only managers and admins can view department tickets");
        }

        if (currentEmployee.getRole() == UserRole.MANAGER) {
            if (currentEmployee.getDepartment() == null) {
                throw new BadRequestException("Manager is not assigned to a department");
            }
            return currentEmployee.getDepartment().getId();
        }

        // ADMIN role
        if (departmentId == null) {
            throw new BadRequestException("Department ID is required");
        }

        if (!departmentRepository.existsById(departmentId)) {
            throw new ResourceNotFoundException("Department not found: " + departmentId);
        }

        return departmentId;
    }

    private Long resolveDepartmentForManager(Long employeeId, Employee currentEmployee) {
        if (currentEmployee.getRole() != UserRole.MANAGER && currentEmployee.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("Only managers and admins can view department tickets");
        }

        if (currentEmployee.getRole() == UserRole.MANAGER) {
            if (currentEmployee.getDepartment() == null) {
                throw new BadRequestException("Manager is not assigned to a department");
            }
            Long managerDepartmentId = currentEmployee.getDepartment().getId();

            if (employeeId != null && !employeeId.equals(currentEmployee.getId())) {
                Employee targetEmployee = employeeRepository.findById(employeeId)
                        .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));

                if (targetEmployee.getDepartment() == null
                        || !managerDepartmentId.equals(targetEmployee.getDepartment().getId())) {
                    throw new AccessDeniedException("You are not authorized to view tickets from this department");
                }
            }
            return managerDepartmentId;
        }

        // ADMIN role
        if (employeeId != null) {
            Employee targetEmployee = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
            return targetEmployee.getDepartment() != null ? targetEmployee.getDepartment().getId() : null;
        }

        return currentEmployee.getDepartment() != null ? currentEmployee.getDepartment().getId() : null;
    }

    private Employee resolveCreatedTargetEmployee(Long employeeId, Employee currentEmployee) {
        if (employeeId == null || employeeId.equals(currentEmployee.getId())) {
            return currentEmployee;
        }

        if (currentEmployee.getRole() != UserRole.MANAGER && currentEmployee.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("You are not authorized to view another employee's tickets");
        }

        Employee targetEmployee = employeeRepository
                .findById(employeeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Employee not found: " + employeeId
                        )
                );

        if (currentEmployee.getRole() == UserRole.MANAGER) {
            if (currentEmployee.getDepartment() == null
                    || targetEmployee.getDepartment() == null
                    || !currentEmployee.getDepartment().getId()
                    .equals(targetEmployee.getDepartment().getId())) {
                throw new AccessDeniedException("You are not authorized to view this employee's tickets");
            }
        }

        return targetEmployee;
    }

    private DepartmentAgent resolveAssignedTargetAgent(Long employeeId, Employee currentEmployee) {
        if (currentEmployee.getRole() == UserRole.EMPLOYEE) {
            throw new AccessDeniedException("Employees cannot view assigned tickets");
        }

        DepartmentAgent agent;
        if (employeeId == null || employeeId.equals(currentEmployee.getId())) {
            agent = departmentAgentRepository
                    .findByEmployeeId(currentEmployee.getId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "No agent assignment found for employee: " + currentEmployee.getId()
                            )
                    );
        } else {
            if (currentEmployee.getRole() != UserRole.MANAGER && currentEmployee.getRole() != UserRole.ADMIN) {
                throw new AccessDeniedException("You are not authorized to view another agent's tickets");
            }

            agent = departmentAgentRepository
                    .findByEmployeeId(employeeId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "No agent assignment found for employee: " + employeeId
                            )
                    );

            if (currentEmployee.getRole() == UserRole.MANAGER) {
                if (currentEmployee.getDepartment() == null
                        || agent.getEmployee() == null
                        || agent.getEmployee().getDepartment() == null
                        || !currentEmployee.getDepartment().getId()
                        .equals(agent.getEmployee().getDepartment().getId())) {
                    throw new AccessDeniedException("You are not authorized to view this agent's tickets");
                }
            }
        }

        return agent;
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('EMPLOYEE')")
    public TicketUpdateResponseDTO withdrawTicket(Long ticketId, WithdrawRequestDTO withdrawRequest) {

        String reason = withdrawRequest.reason();

        long employeeId = authService.getCurrentEmployeeId();

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
            log.warn("Withdraw ticket rejected: ticketId={} is already RESOLVED", ticketId);
            throw new InvalidStateException(
                    "Resolved ticket cannot be withdrawn"
            );
        }

        if (ticket.getStatus() == TicketStatus.WITHDRAWN) {
            log.warn("Withdraw ticket rejected: ticketId={} is already WITHDRAWN", ticketId);
            throw new InvalidStateException(
                    "Ticket is already withdrawn"
            );
        }

        TicketStatus oldStatus = ticket.getStatus();

        // Change ticket status
        ticket.setStatus(TicketStatus.WITHDRAWN);

        if (reason == null || reason.isBlank()) {
            throw new BadRequestException("Withdrawal reason is required");
        }

        ticket.setWithdrawnAt(Instant.now());

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
            slaInstance.setNextEventType(null);
            slaInstance.setNextEventAt(null);
            slaInstanceRepository.save(slaInstance);
        }

        if (ticket.getAssignedAgent() != null && ticket.getAssignedAgent().getEmployee() != null) {
            String ticketNumber = ticket.getTicketNumber() != null ? ticket.getTicketNumber() : ("#" + ticket.getId());
            notificationService.sendNotification(
                    ticket.getAssignedAgent().getEmployee().getId(),
                    ticket.getId(),
                    NotificationType.TICKET_WITHDRAWN,
                    "Ticket Withdrawn",
                    "Ticket " + ticketNumber + " has been withdrawn."
            );
        }

        log.info("Ticket withdrawn successfully: ticketId={}, ticketNumber={}, requesterId={}",
                ticket.getId(), ticket.getTicketNumber(), employeeId);

        return ticketMapper.toUpdateResponse(
                ticket,
                slaInstance
        );
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('AGENT')")
    public TicketUpdateResponseDTO startTicket(Long ticketId) {

        long currentAgentId = authService.getCurrentEmployeeId();

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));

        if (ticket.getAssignedAgent() == null) {
            log.warn("Start ticket rejected: ticketId={} is not assigned to any agent", ticketId);
            throw new InvalidStateException("Ticket is not assigned to any agent");
        }

        boolean isCurrentAgent = ticket.getAssignedAgent().getEmployee() != null
                && ticket.getAssignedAgent().getEmployee().getId().equals(currentAgentId);

        if (!isCurrentAgent) {
            throw new AccessDeniedException("You are not allowed to start this ticket");
        }

        if (ticket.getStatus() != TicketStatus.OPEN && ticket.getStatus() != TicketStatus.REOPENED) {
            log.warn("Start ticket rejected: ticketId={} is in status {}", ticketId, ticket.getStatus());
            throw new InvalidStateException("Ticket is not in OPEN or REOPENED status");
        }

        TicketStatus oldStatus = ticket.getStatus();

        ticket.setStatus(IN_PROGRESS);
        ticketRepository.save(ticket);

        ticketHistoryService.record(
                ticket,
                HistoryEventType.STATUS_CHANGED,
                oldStatus,
                IN_PROGRESS
        );

        SlaInstance slaInstance = slaInstanceRepository.findLatestByTicketId(ticketId);

        String ticketNumber = ticket.getTicketNumber() != null ? ticket.getTicketNumber() : ("#" + ticket.getId());
        String agentName = ticket.getAssignedAgent().getEmployee() != null
                ? ticket.getAssignedAgent().getEmployee().getFirstName()
                : "agent";
        notificationService.sendNotification(
                ticket.getRequester().getId(),
                ticket.getId(),
                NotificationType.TICKET_ON_HOLD,
                "Ticket In Progress",
                "Ticket " + ticketNumber + " has been started by agent " + agentName
        );

        log.info("Ticket started (IN_PROGRESS): ticketId={}, ticketNumber={}, agentId={}",
                ticket.getId(), ticket.getTicketNumber(), currentAgentId);

        return ticketMapper.toUpdateResponse(ticket, slaInstance);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('AGENT')")
    public TicketUpdateResponseDTO holdTicket(Long ticketId, HoldTicketRequestDTO request) {
        long currentAgentId = authService.getCurrentEmployeeId();

        String reason = request != null ? request.reason() : null;
        if (reason == null || reason.isBlank()) {
            throw new BadRequestException("Hold reason is required");
        }

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));

        if (ticket.getAssignedAgent() == null) {
            log.warn("Hold ticket rejected: ticketId={} is not assigned to any agent", ticketId);
            throw new InvalidStateException("Ticket is not assigned to any agent");
        }

        boolean isCurrentAgent = ticket.getAssignedAgent().getEmployee() != null
                && ticket.getAssignedAgent().getEmployee().getId().equals(currentAgentId);

        if (!isCurrentAgent) {
            throw new AccessDeniedException("You are not allowed to hold this ticket");
        }

        if (ticket.getStatus() != IN_PROGRESS) {
            log.warn("Hold ticket rejected: ticketId={} is in status {}", ticketId, ticket.getStatus());
            throw new InvalidStateException("Ticket is not in IN_PROGRESS status");
        }

        ticket.setStatus(ON_HOLD);
        ticket.setHoldReason(reason);
        ticket.setHoldStartedAt(Instant.now());
        ticketRepository.save(ticket);

        ticketHistoryService.record(
                ticket,
                HistoryEventType.HOLD,
                IN_PROGRESS,
                ON_HOLD
        );

        SlaInstance slaInstance = slaService.pauseSla(ticket);

        String ticketNumber = ticket.getTicketNumber() != null ? ticket.getTicketNumber() : ("#" + ticket.getId());
        notificationService.sendNotification(
                ticket.getRequester().getId(),
                ticket.getId(),
                NotificationType.TICKET_ON_HOLD,
                "Ticket On Hold",
                "Ticket " + ticketNumber + " has been put on hold."
        );

        log.info("Ticket put ON_HOLD: ticketId={}, ticketNumber={}, agentId={}",
                ticket.getId(), ticket.getTicketNumber(), currentAgentId);

        return ticketMapper.toUpdateResponse(ticket, slaInstance);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('AGENT')")
    public TicketUpdateResponseDTO resumeTicket(Long ticketId) {
        long currentAgentId = authService.getCurrentEmployeeId();

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));

        if (ticket.getAssignedAgent() == null) {
            log.warn("Resume ticket rejected: ticketId={} is not assigned to any agent", ticketId);
            throw new InvalidStateException("Ticket is not assigned to any agent");
        }

        boolean isCurrentAgent = ticket.getAssignedAgent().getEmployee() != null
                && ticket.getAssignedAgent().getEmployee().getId().equals(currentAgentId);

        if (!isCurrentAgent) {
            throw new AccessDeniedException("You are not allowed to resume this ticket");
        }

        if (ticket.getStatus() != ON_HOLD) {
            log.warn("Resume ticket rejected: ticketId={} is in status {}", ticketId, ticket.getStatus());
            throw new InvalidStateException("Ticket is not in ON_HOLD status");
        }

        ticket.setStatus(IN_PROGRESS);
        ticket.setHoldReason(null);
        ticket.setHoldStartedAt(null);
        ticketRepository.save(ticket);

        ticketHistoryService.record(
                ticket,
                HistoryEventType.RESUMED,
                ON_HOLD,
                IN_PROGRESS
        );

        SlaInstance slaInstance = slaService.resumeSla(ticket);

        String ticketNumber = ticket.getTicketNumber() != null ? ticket.getTicketNumber() : ("#" + ticket.getId());
        notificationService.sendNotification(
                ticket.getRequester().getId(),
                ticket.getId(),
                NotificationType.TICKET_RESUMED,
                "Ticket Resumed",
                "Ticket " + ticketNumber + " has been resumed."
        );

        log.info("Ticket resumed (IN_PROGRESS): ticketId={}, ticketNumber={}, agentId={}",
                ticket.getId(), ticket.getTicketNumber(), currentAgentId);

        return ticketMapper.toUpdateResponse(ticket, slaInstance);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('AGENT')")
    public TicketUpdateResponseDTO resolveTicket(Long ticketId, ResolveTicketRequestDTO request) {
        long currentAgentId = authService.getCurrentEmployeeId();

        String summary = request != null ? request.resolutionSummary() : null;
        if (summary == null || summary.isBlank()) {
            throw new BadRequestException("Resolution summary is required");
        }

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));

        if (ticket.getAssignedAgent() == null) {
            log.warn("Resolve ticket rejected: ticketId={} is not assigned to any agent", ticketId);
            throw new InvalidStateException("Ticket is not assigned to any agent");
        }

        boolean isCurrentAgent = ticket.getAssignedAgent().getEmployee() != null
                && ticket.getAssignedAgent().getEmployee().getId().equals(currentAgentId);

        if (!isCurrentAgent) {
            throw new AccessDeniedException("You are not allowed to resolve this ticket");
        }

        if (ticket.getStatus() != IN_PROGRESS) {
            log.warn("Resolve ticket rejected: ticketId={} is in status {}", ticketId, ticket.getStatus());
            throw new InvalidStateException("Ticket is not in IN_PROGRESS status");
        }

        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setResolutionSummary(summary);
        ticket.setResolvedAt(Instant.now());
        ticketRepository.save(ticket);

        ticketHistoryService.record(
                ticket,
                HistoryEventType.RESOLVED,
                IN_PROGRESS,
                TicketStatus.RESOLVED
        );

        SlaInstance slaInstance = slaService.completeSla(ticket);

        String ticketNumber = ticket.getTicketNumber() != null ? ticket.getTicketNumber() : ("#" + ticket.getId());
        notificationService.sendNotification(
                ticket.getRequester().getId(),
                ticket.getId(),
                NotificationType.TICKET_RESOLVED,
                "Ticket Resolved",
                "Ticket " + ticketNumber + " has been resolved."
        );

        log.info("Ticket resolved: ticketId={}, ticketNumber={}, agentId={}",
                ticket.getId(), ticket.getTicketNumber(), currentAgentId);

        return ticketMapper.toUpdateResponse(ticket, slaInstance);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('EMPLOYEE')")
    public TicketUpdateResponseDTO reopenTicket(Long ticketId, ReopenRequestDTO request) {

        Long employeeId = authService.getCurrentEmployeeId();

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Ticket not found with id: " + ticketId
                        ));

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Employee not found with id: " + employeeId
                        ));

        if (!ticket.getRequester().getId().equals(employee.getId())) {
            throw new AccessDeniedException(
                    "Only the ticket requester can reopen the ticket"
            );
        }

        if (ticket.getStatus() != TicketStatus.RESOLVED) {
            log.warn("Reopen ticket rejected: ticketId={} is not in RESOLVED status (status={})", ticketId, ticket.getStatus());
            throw new InvalidStateException(
                    "Only resolved tickets can be reopened"
            );
        }

        if (ticket.getReopenCount() >= 2) {
            log.warn("Reopen ticket rejected: ticketId={} already reopened {} times", ticketId, ticket.getReopenCount());
            throw new InvalidStateException(
                    "Ticket can only be reopened twice"
            );
        }

        if (ticket.getAssignedAgent() == null) {
            log.warn("Reopen ticket rejected: ticketId={} has no previous assigned agent", ticketId);
            throw new InvalidStateException(
                    "Cannot reopen ticket because no previous agent is assigned"
            );
        }

        int oldReopenCount = ticket.getReopenCount();

        ticket.setReopenCount(oldReopenCount + 1);
        ticket.setStatus(TicketStatus.REOPENED);
        ticket.setReopenedAt(Instant.now());
        ticket.setResolvedAt(null);
        ticket.setResolutionSummary(null);

        ticketRepository.save(ticket);

        ticketHistoryService.record(
                ticket,
                HistoryEventType.REOPENED,
                TicketStatus.RESOLVED,
                TicketStatus.REOPENED
        );

        SlaInstance slaInstance = slaService.startReopenSla(ticket);

        String ticketNumber = ticket.getTicketNumber() != null
                ? ticket.getTicketNumber()
                : ("#" + ticket.getId());

        if (ticket.getAssignedAgent() != null
                && ticket.getAssignedAgent().getEmployee() != null) {

            notificationService.sendNotification(
                    ticket.getAssignedAgent().getEmployee().getId(),
                    ticket.getId(),
                    NotificationType.TICKET_REOPENED,
                    "Ticket Reopened",
                    "Ticket " + ticketNumber + " has been reopened."
            );
        }

        log.info("Ticket reopened: ticketId={}, ticketNumber={}, requesterId={}, newReopenCount={}",
                ticket.getId(), ticket.getTicketNumber(), employeeId, ticket.getReopenCount());

        return ticketMapper.toUpdateResponse(ticket, slaInstance);
    }

    // Category Validation

    private Category findAndValidateCategory(
            CreateTicketRequestDTO request,
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
            CreateTicketRequestDTO request,
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

    private Page<TicketResponseDTO> buildTicketResponse(Page<Ticket> tickets) {
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


    //feedback


    @Override
    @Transactional
    public TicketFeedbackCreateResponseDTO createFeedback(Long ticketId, TicketFeedbackRequestDTO request) {
        long currentEmployeeId = authService.getCurrentEmployeeId();

        if (request == null || request.rating() == null) {
            throw new BadRequestException("Rating is required");
        }
        if (request.rating() < 1 || request.rating() > 5) {
            throw new BadRequestException("Rating must be between 1 and 5");
        }

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));

        if (!ticket.getRequester().getId().equals(currentEmployeeId)) {
            throw new AccessDeniedException("Only the ticket requester can submit feedback");
        }

        if (ticket.getStatus() != TicketStatus.RESOLVED) {
            throw new InvalidStateException("Feedback can only be submitted for resolved tickets");
        }

        if (Boolean.TRUE.equals(ticketFeedbackRepository.existsByTicketId(ticketId))) {
            throw new DuplicateResourceException("Feedback has already been submitted for this ticket");
        }

        Employee submitter = employeeRepository.findById(currentEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + currentEmployeeId));


        TicketFeedback feedback = TicketFeedback.builder()
                .ticket(ticket)
                .submittedBy(submitter)
                .rating(request.rating())
                .comment(request.comment())
                .build();

        feedback = ticketFeedbackRepository.save(feedback);

        ticketHistoryService.record(
                ticket,
                HistoryEventType.FEEDBACK_SUBMITTED,
                ticket.getStatus(),
                ticket.getStatus()
        );

        String ticketNumber = ticket.getTicketNumber() != null ? ticket.getTicketNumber() : ("#" + ticket.getId());
        if (ticket.getAssignedAgent() != null && ticket.getAssignedAgent().getEmployee() != null) {
            notificationService.sendNotification(
                    ticket.getAssignedAgent().getEmployee().getId(),
                    ticket.getId(),
                    NotificationType.FEEDBACK_RECEIVED,
                    "Feedback Received",
                    "Feedback has been submitted for ticket " + ticketNumber + "."
            );
        }

        log.info("Feedback submitted successfully: ticketId={}, ticketNumber={}, rating={}, submittedBy={}",
                ticket.getId(), ticket.getTicketNumber(), request.rating(), currentEmployeeId);

        return ticketFeedbackMapper.toCreateResponse(feedback);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketFeedbackResponseDTO getFeedback(Long ticketId) {

        long currentEmployeeId = authService.getCurrentEmployeeId();

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Ticket not found: " + ticketId));

        Employee currentEmployee = employeeRepository.findById(currentEmployeeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Employee not found: " + currentEmployeeId));

        // 1. Ticket requester
        boolean isRequester = ticket.getRequester() != null
                && ticket.getRequester().getId().equals(currentEmployeeId);

        // 2. Assigned agent
        boolean isAssignedAgent = ticket.getAssignedAgent() != null
                && ticket.getAssignedAgent().getEmployee() != null
                && ticket.getAssignedAgent().getEmployee().getId().equals(currentEmployeeId);

        // 3. Manager of the ticket's department
        boolean isDepartmentManager =
                currentEmployee.getRole() == UserRole.MANAGER
                        && currentEmployee.getDepartment() != null
                        && ticket.getDepartment() != null
                        && currentEmployee.getDepartment().getId()
                        .equals(ticket.getDepartment().getId());

        // 4. Admin
        boolean isAdmin = currentEmployee.getRole() == UserRole.ADMIN;

        // Authorization
        if (!isAdmin && !isRequester && !isAssignedAgent && !isDepartmentManager) {
            throw new AccessDeniedException(
                    "You are not allowed to view feedback for this ticket"
            );
        }

        TicketFeedback feedback = ticketFeedbackRepository.findByTicketId(ticketId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Feedback not found for ticket: " + ticketId
                        ));

        return ticketFeedbackMapper.toResponse(feedback);
    }

    @PreAuthorize("hasRole('MANAGER')")
    @Override
    @Transactional
    public TicketUpdateResponseDTO assignTicketByManager(Long ticketId, Long agentId) {

        if (agentId == null) {
            throw new BadRequestException("Agent ID is required");
        }

        Long currentEmployeeId = authService.getCurrentEmployeeId();

        Employee currentEmployee = employeeRepository.findById(currentEmployeeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Employee not found: " + currentEmployeeId));

        Ticket ticket = ticketRepository.findByIdWithLock(ticketId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Ticket not found: " + ticketId));

        // Cannot assign resolved or withdrawn tickets
        if (ticket.getStatus() == TicketStatus.RESOLVED
                || ticket.getStatus() == TicketStatus.WITHDRAWN) {
            log.warn("Ticket assignment rejected: ticketId={} is in status {}", ticketId, ticket.getStatus());
            throw new InvalidStateException(
                    "Cannot assign a " + ticket.getStatus() + " ticket");
        }

        Long ticketDeptId = ticket.getDepartment().getId();

        // Manager can only assign tickets belonging to their department
        if (currentEmployee.getDepartment() == null
                || !ticketDeptId.equals(currentEmployee.getDepartment().getId())) {
            log.warn("Ticket assignment rejected: manager employeeId={} from departmentId={} cannot assign ticketId={} in departmentId={}",
                    currentEmployeeId, currentEmployee.getDepartment() != null ? currentEmployee.getDepartment().getId() : null, ticketId, ticketDeptId);
            throw new AccessDeniedException(
                    "You are not authorized to assign tickets for this department");
        }

        // Find the selected agent
        DepartmentAgent agent = departmentAgentRepository.findById(agentId)
                .or(() -> departmentAgentRepository.findByEmployeeId(agentId))
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Agent not found: " + agentId));

        Employee agentEmployee = agent.getEmployee();

        // Agent must belong to the same department as the ticket
        if (agentEmployee.getDepartment() == null
                || !ticketDeptId.equals(agentEmployee.getDepartment().getId())) {
            log.warn("Ticket assignment rejected: agent employeeId={} from departmentId={} cannot be assigned to ticketId={} in departmentId={}",
                    agentEmployee.getId(), agentEmployee.getDepartment() != null ? agentEmployee.getDepartment().getId() : null, ticketId, ticketDeptId);
            throw new BadRequestException(
                    "Selected agent does not belong to the ticket's department");
        }

        // Agent must be active
        if (agentEmployee.getEmploymentStatus() != EmploymentStatus.ACTIVE) {
            log.warn("Ticket assignment rejected: agent employeeId={} is inactive", agentEmployee.getId());
            throw new BadRequestException(
                    "Selected agent is not active");
        }

        // Get latest SLA
        SlaInstance latestSla =
                slaInstanceRepository.findLatestByTicketId(ticket.getId());

        boolean isSlaBreached =
                latestSla != null
                        && latestSla.getStatus() == SlaStatus.BREACHED;

        // Check whether this is a reassignment
        boolean isReassignment = ticket.getAssignedAgent() != null;

        if (isReassignment) {

            // Reassignment is allowed only after SLA breach
            if (!isSlaBreached) {
                log.warn("Ticket reassignment rejected: ticketId={} SLA is not breached", ticketId);
                throw new InvalidStateException(
                        "Ticket is already assigned to an agent and SLA is not breached");
            }

            // Cannot assign to the same agent
            if (ticket.getAssignedAgent().getId().equals(agent.getId())) {
                log.warn("Ticket reassignment rejected: ticketId={} is already assigned to agentId={}", ticketId, agent.getId());
                throw new BadRequestException(
                        "Ticket is already assigned to this agent");
            }
        }

        Instant now = Instant.now();

        // Assign ticket
        ticket.setAssignedAgent(agent);
        ticket.setAssignedAt(now);

        // Determine assigned manager
        DepartmentManager assignedManager = null;

        if (agent.getEmployee() != null
                && agent.getEmployee().getManager() != null) {

            assignedManager = agent.getEmployee().getManager();
        }

        if (assignedManager == null) {

            assignedManager = departmentManagerRepository
                    .findByEmployeeId(currentEmployee.getId())
                    .orElse(null);
        }

        ticket.setAssignedManager(assignedManager);

        // Update agent's last assignment time
        agent.setLastAssignedAt(now);

        // If SLA is breached, start a new 50% SLA cycle
        SlaInstance currentSla;
        if (isSlaBreached) {
            currentSla = slaService.startBreachRecoverySla(ticket);
        } else {
            currentSla = latestSla;
        }

        // Create history event
        HistoryEventType historyEvent = isReassignment
                ? HistoryEventType.REASSIGNED
                : HistoryEventType.ASSIGNED;

        ticketHistoryService.record(
                ticket,
                historyEvent,
                ticket.getStatus(),
                ticket.getStatus()
        );

        // Create notification
        NotificationType notificationType = isReassignment
                ? NotificationType.TICKET_REASSIGNED
                : NotificationType.TICKET_ASSIGNED;

        String ticketNumber = ticket.getTicketNumber() != null
                ? ticket.getTicketNumber()
                : "#" + ticket.getId();

        String title = isReassignment
                ? "Ticket Reassigned"
                : "Ticket Assigned";

        String message = isReassignment
                ? "Ticket " + ticketNumber + " has been reassigned to you."
                : "Ticket " + ticketNumber + " has been assigned to you.";

        notificationService.sendNotification(
                agentEmployee.getId(),
                ticket.getId(),
                notificationType,
                title,
                message
        );

        log.info("Ticket {} by manager: ticketId={}, ticketNumber={}, agentId={}, managerId={}",
                isReassignment ? "reassigned" : "assigned", ticket.getId(), ticket.getTicketNumber(), agent.getId(),
                assignedManager != null ? assignedManager.getId() : null);

        return ticketMapper.toUpdateResponse(ticket, currentSla);
    }

    @Override
    @Transactional
    public TicketUpdateResponseDTO updateTicket(
            Long ticketId,
            UpdateTicketRequestDTO request
    ) {

        switch (request.status()) {

            case IN_PROGRESS:
                return startTicket(ticketId);

            case ON_HOLD:
                return holdTicket(
                        ticketId,
                        new HoldTicketRequestDTO(request.reason())
                );

            case RESOLVED:
                return resolveTicket(
                        ticketId,
                        new ResolveTicketRequestDTO(request.resolution())
                );

            case WITHDRAWN:
                return withdrawTicket(
                        ticketId,
                        new WithdrawRequestDTO(request.reason())
                );

            case REOPENED:
                return reopenTicket(
                        ticketId,
                        new ReopenRequestDTO(request.reason()));

            case RESUME:
                return resumeTicket(ticketId);

            case ASSIGNED:
            case ASSIGN:
            case REASSIGN:
                return assignTicketByManager(ticketId, request.agentId());

            default:
                throw new BadRequestException(
                        "Unsupported ticket status: " + request.status()
                );
        }
    }

    private TicketResponseDTO buildTicketResponse(Ticket ticket) {

        SlaInstance slaInstance =
                slaInstanceRepository.findLatestByTicketId(ticket.getId());

        return ticketMapper.toResponse(ticket, slaInstance);
    }
}