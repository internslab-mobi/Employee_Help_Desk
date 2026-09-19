package com.example.helpdesk.service.impl;

import com.example.helpdesk.dto.request.AssignTicketRequest;
import com.example.helpdesk.dto.request.CreateTicketRequest;
import com.example.helpdesk.dto.request.ReopenTicketRequest;
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
import com.example.helpdesk.enums.TicketStatus;
import com.example.helpdesk.mapper.TicketMapper;
import com.example.helpdesk.repository.CategoryRepository;
import com.example.helpdesk.repository.DepartmentAgentRepository;
import com.example.helpdesk.repository.DepartmentRepository;
import com.example.helpdesk.repository.EmployeeRepository;
import com.example.helpdesk.repository.SubCategoryRepository;
import com.example.helpdesk.repository.TicketRepository;
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
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));
        ticket.setStatus(request.getStatus());
        Ticket savedTicket = ticketRepository.save(ticket);
        log.info("Ticket status updated. ticketId={}, newStatus={}", ticketId, request.getStatus());
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
    public TicketResponse resolveTicket(Long ticketId, UpdateTicketStatusRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));
        ticket.setStatus(request.getStatus());
        ticket.setResolvedAt(LocalDateTime.now());
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
}
