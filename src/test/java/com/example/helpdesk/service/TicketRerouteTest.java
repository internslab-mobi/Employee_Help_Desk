package com.example.helpdesk.service;

import com.example.helpdesk.dto.request.UpdateTicketRequestDTO;
import com.example.helpdesk.dto.response.TicketResponseDTO;
import com.example.helpdesk.entity.*;
import com.example.helpdesk.enums.TicketEventType;
import com.example.helpdesk.enums.TicketPatchOperation;
import com.example.helpdesk.enums.TicketStatus;
import com.example.helpdesk.repository.*;
import com.example.helpdesk.service.impl.TicketServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketRerouteTest {

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private DepartmentAgentRepository departmentAgentRepository;
    @Mock
    private DepartmentManagerRepository departmentManagerRepository;
    @Mock
    private TicketSlaRepository ticketSlaRepository;
    @Mock
    private SubCategorySkillRepository subCategorySkillRepository;
    @Mock
    private AgentSkillRepository agentSkillRepository;
    @Mock
    private SlaService slaService;
    @Mock
    private TicketHistoryService ticketHistoryService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private com.example.helpdesk.util.AuthenticatedEmployeeUtil authenticatedEmployeeUtil;

    @InjectMocks
    private TicketServiceImpl ticketService;

    private Ticket openTicket;
    private Department department;
    private Category category;
    private SubCategory subCategory;
    private Employee requester;
    private Employee agentEmployee;
    private Employee managerEmployee;
    private DepartmentAgent departmentAgent;
    private DepartmentManager departmentManager;
    private Skill skill;

    @BeforeEach
    void setUp() {
        department = Department.builder()
                .id(1L)
                .code("IT")
                .name("IT")
                .timezone("UTC")
                .build();

        category = Category.builder()
                .id(5L)
                .name("Hardware")
                .department(department)
                .build();

        subCategory = SubCategory.builder()
                .id(1L)
                .name("Laptop/Desktop Hardware")
                .category(category)
                .priority("HIGH")
                .build();

        requester = Employee.builder()
                .id(2L)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .employeeCode("EMP002")
                .department(department)
                .build();

        agentEmployee = Employee.builder()
                .id(7L)
                .firstName("Agent")
                .lastName("Seven")
                .email("agent7@example.com")
                .employeeCode("EMP007")
                .department(department)
                .build();

        managerEmployee = Employee.builder()
                .id(4L)
                .firstName("Manager")
                .lastName("Four")
                .email("manager4@example.com")
                .employeeCode("EMP004")
                .department(department)
                .build();

        departmentAgent = DepartmentAgent.builder()
                .id(1L)
                .department(department)
                .employee(agentEmployee)
                .active(true)
                .build();

        departmentManager = DepartmentManager.builder()
                .id(1L)
                .department(department)
                .employee(managerEmployee)
                .primary(true)
                .build();

        skill = Skill.builder()
                .id(1L)
                .name("Hardware Troubleshooting")
                .build();

        openTicket = Ticket.builder()
                .id(5L)
                .ticketNumber("TKT-TEST-0005")
                .department(department)
                .category(category)
                .subCategory(subCategory)
                .requester(requester)
                .subject("Laptop issue")
                .description("Hardware failure")
                .priority("HIGH")
                .status(TicketStatus.OPEN.name())
                .assignedAgent(null)
                .assignedManager(null)
                .slaPolicy(null)
                .reopenCount(0)
                .createdAt(Instant.now())
                .build();

        lenient().when(authenticatedEmployeeUtil.getAuthenticatedEmployeeTimezone()).thenReturn("UTC");
    }

    @Test
    void testReroute_Success_FullRoutingAndSla() {
        UpdateTicketRequestDTO request = UpdateTicketRequestDTO.builder()
                .operation(TicketPatchOperation.REROUTE)
                .data(JsonNodeFactory.instance.objectNode())
                .build();

        when(ticketRepository.findById(5L)).thenReturn(Optional.of(openTicket));
        when(subCategorySkillRepository.findBySubCategoryId(1L)).thenReturn(
                List.of(SubCategorySkill.builder().id(1L).subCategory(subCategory).skill(skill).build())
        );
        when(departmentAgentRepository.findByDepartmentId(1L)).thenReturn(List.of(departmentAgent));
        when(agentSkillRepository.findByAgentId(1L)).thenReturn(
                List.of(AgentSkill.builder().id(1L).agent(departmentAgent).skill(skill).build())
        );
        when(departmentManagerRepository.findByDepartmentIdAndPrimaryTrue(1L)).thenReturn(Optional.of(departmentManager));

        SlaRule slaRule = SlaRule.builder().id(2L).durationMinutes(240).build();
        TicketSla ticketSla = TicketSla.builder().id(2L).ticket(openTicket).slaPolicy(slaRule).status("RUNNING").build();
        when(slaService.createSlaInstance(openTicket)).thenReturn(ticketSla);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketResponseDTO response = ticketService.updateTicket(5L, request);

        assertNotNull(response);
        assertEquals(5L, response.getId());
        assertNotNull(response.getAssignedAgent());
        assertEquals(7L, response.getAssignedAgent().getId());
        assertEquals(4L, response.getManagerId());

        // Verify history recordings
        verify(ticketHistoryService).recordHistory(
                eq(openTicket),
                eq(requester),
                eq(TicketEventType.ASSIGNMENT_CONFIRMED),
                isNull(),
                eq("1"),
                isNull()
        );
        verify(ticketHistoryService).recordHistory(
                eq(openTicket),
                eq(requester),
                eq(TicketEventType.SLA_STARTED),
                isNull(),
                isNull(),
                isNull()
        );
    }

    @Test
    void testReroute_Success_NoSlaFound_DoesNotRecordSlaStarted() {
        UpdateTicketRequestDTO request = UpdateTicketRequestDTO.builder()
                .operation(TicketPatchOperation.REROUTE)
                .data(JsonNodeFactory.instance.objectNode())
                .build();

        when(ticketRepository.findById(5L)).thenReturn(Optional.of(openTicket));
        when(subCategorySkillRepository.findBySubCategoryId(1L)).thenReturn(
                List.of(SubCategorySkill.builder().id(1L).subCategory(subCategory).skill(skill).build())
        );
        when(departmentAgentRepository.findByDepartmentId(1L)).thenReturn(List.of(departmentAgent));
        when(agentSkillRepository.findByAgentId(1L)).thenReturn(
                List.of(AgentSkill.builder().id(1L).agent(departmentAgent).skill(skill).build())
        );
        when(departmentManagerRepository.findByDepartmentIdAndPrimaryTrue(1L)).thenReturn(Optional.of(departmentManager));
        when(slaService.createSlaInstance(openTicket)).thenReturn(null);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketResponseDTO response = ticketService.updateTicket(5L, request);

        assertNotNull(response);
        verify(ticketHistoryService).recordHistory(
                eq(openTicket),
                eq(requester),
                eq(TicketEventType.ASSIGNMENT_CONFIRMED),
                isNull(),
                eq("1"),
                isNull()
        );
        verify(ticketHistoryService, never()).recordHistory(
                any(),
                any(),
                eq(TicketEventType.SLA_STARTED),
                any(),
                any(),
                any()
        );
    }

    @Test
    void testReroute_Fails_WhenTicketNotOpen() {
        openTicket.setStatus(TicketStatus.CLOSED.name());
        when(ticketRepository.findById(5L)).thenReturn(Optional.of(openTicket));

        UpdateTicketRequestDTO request = UpdateTicketRequestDTO.builder()
                .operation(TicketPatchOperation.REROUTE)
                .data(JsonNodeFactory.instance.objectNode())
                .build();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                ticketService.updateTicket(5L, request)
        );
        assertTrue(ex.getMessage().contains("status must be OPEN"));
    }

    @Test
    void testReroute_Fails_WhenAgentAlreadyAssigned() {
        openTicket.setAssignedAgent(departmentAgent);
        when(ticketRepository.findById(5L)).thenReturn(Optional.of(openTicket));

        UpdateTicketRequestDTO request = UpdateTicketRequestDTO.builder()
                .operation(TicketPatchOperation.REROUTE)
                .data(JsonNodeFactory.instance.objectNode())
                .build();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                ticketService.updateTicket(5L, request)
        );
        assertTrue(ex.getMessage().contains("already has an assigned agent"));
    }
}
