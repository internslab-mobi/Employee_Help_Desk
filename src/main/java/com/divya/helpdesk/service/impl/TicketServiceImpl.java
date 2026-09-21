package com.divya.helpdesk.service.impl;

import com.divya.helpdesk.dto.request.TicketCreateRequest;
import com.divya.helpdesk.dto.request.TicketResolveRequest;
import com.divya.helpdesk.dto.request.TicketUpdateRequest;
import com.divya.helpdesk.dto.response.TicketDetailsResponse;
import com.divya.helpdesk.dto.response.TicketResponse;
import com.divya.helpdesk.entity.*;
import com.divya.helpdesk.enums.HDPriorityLevel;
import com.divya.helpdesk.enums.HDTicketStatus;
import com.divya.helpdesk.exception.BadRequestException;
import com.divya.helpdesk.exception.ResourceNotFoundException;
import com.divya.helpdesk.mapper.TicketMapper;
import com.divya.helpdesk.repository.*;
import com.divya.helpdesk.service.SlaService;
import com.divya.helpdesk.service.TicketAssignmentService;
import com.divya.helpdesk.service.TicketService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final HDSlaInstanceRepository slaInstanceRepository;
    private final TicketAssignmentService ticketAssignmentService;
    private final SlaService slaService;
    private final TicketMapper ticketMapper;

    @Override
    public TicketResponse createTicket(TicketCreateRequest request, Long employeeId) {

        // get employee by their id
        HDEmployee requester = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Requester not found with id: " + employeeId));

        // get the department by id
        HDDepartment department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.getDepartmentId()));

        // get the category
        HDCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));

        // get the sub category
        HDSubCategory subCategory = subCategoryRepository.findById(request.getSubCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("SubCategory not found with id: " + request.getSubCategoryId()));

        // Priority is automatically assigned from the database subcategory, if missing keep default as MEDIUM
        HDPriorityLevel priority = subCategory.getPriority() != null ?
                subCategory.getPriority() :
                HDPriorityLevel.MEDIUM;

        // Lookup SLA policy for the selected department and subcategory
        HDSlaPolicy slaPolicy = slaPolicyRepository
                .findByDepartmentIdAndSubCategoryId(department.getId(), subCategory.getId())
                .orElse(null);

        HDTicket ticket = new HDTicket();
        ticket.setTicketNumber(generateTicketNumber());
        ticket.setRequester(requester);
        ticket.setDepartment(department);
        ticket.setCategory(category);
        ticket.setSubCategory(subCategory);
        ticket.setSubject(request.getSubject());
        ticket.setDescription(request.getDescription());
        ticket.setPriority(priority);
        ticket.setSlaPolicy(slaPolicy);
        ticket.setStatus(HDTicketStatus.NEW);

        // Smart agent assignment or manager escalation
        ticketAssignmentService.assignAgentOrEscalate(ticket);

        HDTicket savedTicket = ticketRepository.save(ticket);

        // Create SLA Instance if policy exists
        HDSlaInstance slaInstance = null;
        if (slaPolicy != null) {
            slaInstance = slaService.createSlaInstance(savedTicket, slaPolicy, LocalDateTime.now());
        }

        return ticketMapper.toResponse(savedTicket, slaInstance);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> getTickets(Long employeeId, String role, Long departmentId, Long agentId, HDTicketStatus status, HDPriorityLevel priority) {
        HDEmployee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));

        Specification<HDTicket> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Predicate ownTicket = cb.equal(root.get("requester").get("id"), employeeId);

            if ("ROLE_EMPLOYEE".equals(role)) {
                predicates.add(ownTicket);
            } else if ("ROLE_AGENT".equals(role)) {
                Predicate assignedToMe = cb.equal(root.get("assignedAgent").get("id"), employeeId);
                predicates.add(cb.or(ownTicket, assignedToMe));
            } else if ("ROLE_MANAGER".equals(role)) {
                Predicate departmentTicket = cb.equal(root.get("department").get("id"), employee.getDepartment().getId());
                predicates.add(cb.or(ownTicket, departmentTicket));
            } else if ("ROLE_ADMIN".equals(role)) {
                // No authorization restriction
            } else {
                throw new AccessDeniedException("You are not allowed to view tickets");
            }

            if (departmentId != null) {
                predicates.add(cb.equal(root.get("department").get("id"), departmentId));
            }
            if (agentId != null) {
                predicates.add(cb.equal(root.get("assignedAgent").get("id"), agentId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return ticketRepository.findAll(spec).stream()
                .map(t -> {
                    HDSlaInstance sla = slaInstanceRepository.findByTicketId(t.getId()).orElse(null);
                    return ticketMapper.toResponse(t, sla);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse getTicketById(Long ticketId, Long employeeId) {
        HDTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + ticketId));
        HDEmployee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));

        String role = SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .iterator()
                .next()
                .getAuthority();

        if (!canViewTicket(ticket, employee, role)) {
            throw new AccessDeniedException("You are not allowed to view this ticket");
        }

        HDSlaInstance sla = slaInstanceRepository.findByTicketId(ticketId).orElse(null);
        return ticketMapper.toResponse(ticket, sla);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse getTicketByTicketNumber(String ticketNumber, Long employeeId) {
        HDTicket ticket = ticketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with ticket number: " + ticketNumber));
        HDEmployee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));
        String role = SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .iterator()
                .next()
                .getAuthority();

        if (!canViewTicket(ticket, employee, role)) {
            throw new AccessDeniedException("You are not allowed to view this ticket");
        }
        HDSlaInstance sla = slaInstanceRepository.findByTicketId(ticket.getId()).orElse(null);
        return ticketMapper.toResponse(ticket, sla);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketDetailsResponse getTicketDetails(Long id, Long employeeId) {
        HDTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + id));
        HDEmployee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));
        String role = SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .iterator()
                .next()
                .getAuthority();

        if (!canViewTicket(ticket, employee, role)) {
            throw new AccessDeniedException("You are not allowed to view this ticket");
        }
        HDSlaInstance slaInstance = slaInstanceRepository.findByTicketId(id).orElse(null);
        return ticketMapper.toDetailsResponse(ticket, slaInstance);
    }

    @Override
    public TicketResponse updateTicket(Long ticketId, TicketUpdateRequest request, Long employeeId) {

        // find ticket
        HDTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + ticketId));

        // verify requester owns the ticket
        if (!ticket.getRequester().getId().equals(employeeId)) {
            throw new AccessDeniedException("You are not allowed to update this ticket");
        }

        // don't allow editing a resolved ticket
        if (ticket.getStatus() == HDTicketStatus.RESOLVED || ticket.getStatus() == HDTicketStatus.WITHDRAWN) {
            throw new BadRequestException(ticket.getStatus()+" ticket cannot be edited");
        }

        // find new department
        HDDepartment department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.getDepartmentId()));

        // check department active
        if (!Boolean.TRUE.equals(department.getIsActive())) {
            throw new BadRequestException("Department is inactive");
        }

        // find new category
        HDCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));

        if (!Boolean.TRUE.equals(category.getIsActive())) {
            throw new BadRequestException("Category is inactive");
        }

        // make sure category belongs to department
        if (!category.getDepartment().getId().equals(department.getId())) {
            throw new BadRequestException("Category does not belong to the selected department");
        }

        // find new sub-category
        HDSubCategory subCategory = subCategoryRepository.findById(request.getSubCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Sub-category not found with id: " + request.getSubCategoryId()));

        if (!Boolean.TRUE.equals(subCategory.getIsActive())) {
            throw new BadRequestException("Sub-category is inactive");
        }

        // make sure sub-category belongs to category
        if (!subCategory.getCategory().getId().equals(category.getId())) {
            throw new BadRequestException("Sub-category does not belong to the selected category");
        }

        // find SLA policy for NEW department + sub-category
        HDSlaPolicy slaPolicy = slaPolicyRepository.findByDepartmentIdAndSubCategoryId(department.getId(), subCategory.getId())
                .orElseThrow(() -> new BadRequestException("No active SLA policy configured for the selected department and sub-category"));

        // determine new priority from sub-category or else setting MEDIUM as default
        HDPriorityLevel newPriority = subCategory.getPriority() != null ? subCategory.getPriority() : HDPriorityLevel.MEDIUM;

        // check whether routing information changed
        boolean routingChanged = !ticket.getDepartment().getId().equals(department.getId()) || !ticket.getCategory().getId().equals(category.getId()) || !ticket.getSubCategory().getId().equals(subCategory.getId());

        // update editable fields
        ticket.setDepartment(department);
        ticket.setCategory(category);
        ticket.setSubCategory(subCategory);
        ticket.setSubject(request.getSubject());
        ticket.setDescription(request.getDescription());
        ticket.setPriority(newPriority);
        ticket.setSlaPolicy(slaPolicy);

        /*
         * if department/category/subcategory changed,
         * the old agent may no longer be suitable.
         */
        if (routingChanged) {
            ticket.setAssignedAgent(null);
        }
        // save the updated ticket first
        HDTicket savedTicket = ticketRepository.saveAndFlush(ticket);
        // recalculate SLA
        HDSlaInstance slaInstance = slaInstanceRepository.findByTicketId(savedTicket.getId())
                        .orElse(null);
        LocalDateTime newSlaStart = LocalDateTime.now();
        if (slaInstance != null) {
            slaInstance = slaService.recalculateSlaInstance(slaInstance, savedTicket, slaPolicy, newSlaStart);
        } else {
            slaInstance = slaService.createSlaInstance(savedTicket, slaPolicy, newSlaStart);
        }

        // reassign only if routing changed
        if (routingChanged) {
            ticketAssignmentService.assignAgentOrEscalate(savedTicket);
        }

        return ticketMapper.toResponse(savedTicket, slaInstance);
    }

    @PreAuthorize("hasAnyRole('AGENT', 'MANAGER')")
    @Override
    public TicketResponse startWorkingOnTicket(Long id, Long employeeId) {
        HDTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + id));

        if (ticket.getAssignedAgent() == null || !ticket.getAssignedAgent().getId().equals(employeeId)) {
            throw new AccessDeniedException("You are not assigned to this ticket");
        }

        if (ticket.getStatus() != HDTicketStatus.ASSIGNED && ticket.getStatus() != HDTicketStatus.NEW) {
            throw new BadRequestException("Ticket cannot be transitioned to IN_PROGRESS from status: " + ticket.getStatus());
        }

        ticket.setStatus(HDTicketStatus.IN_PROGRESS);
        HDTicket saved = ticketRepository.save(ticket);
        HDSlaInstance sla = slaInstanceRepository.findByTicketId(id).orElse(null);
        return ticketMapper.toResponse(saved, sla);
    }

    @PreAuthorize("hasAnyRole('AGENT', 'MANAGER')")
    @Override
    public TicketResponse resolveTicket(Long id, TicketResolveRequest request, Long employeeId) {
        HDTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + id));

        if (ticket.getAssignedAgent() == null || !ticket.getAssignedAgent().getId().equals(employeeId)) {
            throw new AccessDeniedException("You are not assigned to this ticket");
        }

        if (ticket.getStatus() == HDTicketStatus.RESOLVED) {
            throw new BadRequestException("Ticket is already " + ticket.getStatus());
        }

        ticket.setStatus(HDTicketStatus.RESOLVED);
        ticket.setResolvedAt(LocalDateTime.now());
        if (request != null && request.getResolutionSummary() != null) {
            ticket.setResolutionSummary(request.getResolutionSummary());
        }

        HDTicket saved = ticketRepository.save(ticket);
        HDSlaInstance sla = slaInstanceRepository.findByTicketId(id).orElse(null);
        return ticketMapper.toResponse(saved, sla);
    }

    @PreAuthorize("hasAnyRole('MANAGER')")
    @Transactional
    @Override
    public void deleteTicket(Long ticketId, Long employeeId) {
        HDTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + ticketId));
        HDEmployee manager = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: "+employeeId));
        if(ticket.getDepartment() == null || manager.getDepartment() == null || !ticket.getDepartment().getId().equals(manager.getDepartment().getId())){
            throw new AccessDeniedException("You are not allowed to delete this ticket");
        }
        slaInstanceRepository.deleteByTicketId(ticketId);
        ticketRepository.deleteById(ticketId);
    }

    private String generateTicketNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "TKT-" + datePart + "-" + randomPart;
    }

    private boolean canViewTicket(HDTicket ticket, HDEmployee employee, String role) {
        Long employeeId = employee.getId();
        if ("ROLE_EMPLOYEE".equals(role)) {
            return ticket.getRequester() != null && ticket.getRequester().getId().equals(employeeId);
        } else if ("ROLE_AGENT".equals(role)) {
            boolean ownTicket = ticket.getRequester() != null && ticket.getRequester().getId().equals(employeeId);
            boolean assignedToMe = ticket.getAssignedAgent() != null && ticket.getAssignedAgent().getId().equals(employeeId);
            return ownTicket || assignedToMe;
        } else if ("ROLE_MANAGER".equals(role)) {
            boolean ownTicket = ticket.getRequester() != null && ticket.getRequester().getId().equals(employeeId);
            boolean departmentTicket = ticket.getDepartment() != null && employee.getDepartment() != null &&
                            ticket.getDepartment().getId().equals(employee.getDepartment().getId());
            return ownTicket || departmentTicket;
        } else if ("ROLE_ADMIN".equals(role)) {
            return true;
        }

        return false;
    }
}
