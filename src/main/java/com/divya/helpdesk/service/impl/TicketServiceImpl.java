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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public TicketResponse createTicket(TicketCreateRequest request) {

        // get employee by their id
        HDEmployee requester = employeeRepository.findById(request.getRequesterId())
                .orElseThrow(() -> new ResourceNotFoundException("Requester not found with id: " + request.getRequesterId()));

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
    public List<TicketResponse> getTickets(Long requesterId, Long departmentId, Long agentId, HDTicketStatus status, HDPriorityLevel priority) {
        Specification<HDTicket> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (requesterId != null) {
                predicates.add(cb.equal(root.get("requester").get("id"), requesterId));
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
    public TicketResponse getTicketById(Long id) {
        HDTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + id));
        HDSlaInstance sla = slaInstanceRepository.findByTicketId(id).orElse(null);
        return ticketMapper.toResponse(ticket, sla);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse getTicketByTicketNumber(String ticketNumber) {
        HDTicket ticket = ticketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with ticket number: " + ticketNumber));
        HDSlaInstance sla = slaInstanceRepository.findByTicketId(ticket.getId()).orElse(null);
        return ticketMapper.toResponse(ticket, sla);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketDetailsResponse getTicketDetails(Long id) {
        HDTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + id));
        HDSlaInstance slaInstance = slaInstanceRepository.findByTicketId(id).orElse(null);
        return ticketMapper.toDetailsResponse(ticket, slaInstance);
    }

    @Override
    public TicketResponse updateTicket(Long ticketId, TicketUpdateRequest request) {

        // find ticket
        HDTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + ticketId));

        // verify requester owns the ticket
        if (!ticket.getRequester().getId().equals(request.getRequesterId())) {
            throw new BadRequestException("You are not allowed to update this ticket");
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

    @Override
    public TicketResponse startWorkingOnTicket(Long id) {
        HDTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + id));

        if (ticket.getStatus() != HDTicketStatus.ASSIGNED && ticket.getStatus() != HDTicketStatus.NEW) {
            throw new BadRequestException("Ticket cannot be transitioned to IN_PROGRESS from status: " + ticket.getStatus());
        }

        ticket.setStatus(HDTicketStatus.IN_PROGRESS);
        HDTicket saved = ticketRepository.save(ticket);
        HDSlaInstance sla = slaInstanceRepository.findByTicketId(id).orElse(null);
        return ticketMapper.toResponse(saved, sla);
    }

    @Override
    public TicketResponse resolveTicket(Long id, TicketResolveRequest request) {
        HDTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + id));

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

    @Override
    public void deleteTicket(Long id) {
        if (!ticketRepository.existsById(id)) {
            throw new ResourceNotFoundException("Ticket not found with id: " + id);
        }
        ticketRepository.deleteById(id);
    }

    private String generateTicketNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "TKT-" + datePart + "-" + randomPart;
    }
}
