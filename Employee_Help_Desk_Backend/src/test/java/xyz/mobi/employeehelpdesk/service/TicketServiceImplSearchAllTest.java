package xyz.mobi.employeehelpdesk.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.databind.ObjectMapper;
import xyz.mobi.employeehelpdesk.dto.ticket.TicketResponseDTO;
import xyz.mobi.employeehelpdesk.entity.Employee;
import xyz.mobi.employeehelpdesk.entity.Ticket;
import xyz.mobi.employeehelpdesk.entity.enums.Priority;
import xyz.mobi.employeehelpdesk.entity.enums.SlaStatus;
import xyz.mobi.employeehelpdesk.entity.enums.TicketStatus;
import xyz.mobi.employeehelpdesk.entity.enums.TicketView;
import xyz.mobi.employeehelpdesk.entity.enums.UserRole;
import xyz.mobi.employeehelpdesk.mapper.TicketAttachmentMapper;
import xyz.mobi.employeehelpdesk.mapper.TicketFeedbackMapper;
import xyz.mobi.employeehelpdesk.mapper.TicketMapper;
import xyz.mobi.employeehelpdesk.repository.*;
import xyz.mobi.employeehelpdesk.security.UserPrincipal;
import xyz.mobi.employeehelpdesk.service.helperservice.TicketHistoryService;
import xyz.mobi.employeehelpdesk.service.impl.TicketServiceImpl;
import xyz.mobi.employeehelpdesk.validator.TicketAttachmentValidator;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplSearchAllTest {

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private TicketAttachmentRepository ticketAttachmentRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private SubCategoryRepository subCategoryRepository;
    @Mock
    private SlaInstanceRepository slaInstanceRepository;
    @Mock
    private TicketMapper ticketMapper;
    @Mock
    private TicketAttachmentMapper ticketAttachmentMapper;
    @Mock
    private TicketAttachmentValidator ticketAttachmentValidator;
    @Mock
    private TicketRoutingService ticketRoutingService;
    @Mock
    private SlaService slaService;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private DepartmentAgentRepository departmentAgentRepository;
    @Mock
    private TicketHistoryService ticketHistoryService;
    @Mock
    private TicketHistoryRepository ticketHistoryRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private TicketFeedbackRepository ticketFeedbackRepository;
    @Mock
    private TicketFeedbackMapper ticketFeedbackMapper;
    @Mock
    private SubCategorySkillRepository subCategorySkillRepository;
    @Mock
    private AgentSkillRepository agentSkillRepository;
    @Mock
    private DepartmentManagerRepository departmentManagerRepository;

    private TicketServiceImpl ticketService;

    @BeforeEach
    void setUp() {
        ticketService = new TicketServiceImpl(
                ticketRepository,
                ticketAttachmentRepository,
                employeeRepository,
                departmentRepository,
                categoryRepository,
                subCategoryRepository,
                slaInstanceRepository,
                ticketMapper,
                ticketAttachmentMapper,
                ticketAttachmentValidator,
                ticketRoutingService,
                slaService,
                objectMapper,
                departmentAgentRepository,
                ticketHistoryService,
                ticketHistoryRepository,
                notificationService,
                ticketFeedbackRepository,
                ticketFeedbackMapper,
                subCategorySkillRepository,
                agentSkillRepository,
                departmentManagerRepository
        );
    }

    @Test
    @DisplayName("searchTickets with TicketView.ALL should throw AccessDeniedException when role is not ADMIN")
    void searchTickets_allView_nonAdmin_throwsAccessDeniedException() {
        Long employeeId = 100L;
        Employee employee = new Employee();
        employee.setId(employeeId);
        employee.setRole(UserRole.EMPLOYEE);

        UserPrincipal employeePrincipal = new UserPrincipal(employeeId, "emp@test.com", "", UserRole.EMPLOYEE, "UTC", true, Collections.emptyList());

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> ticketService.searchTickets(
                        TicketView.ALL,
                        null,
                        null,
                        null,
                        TicketStatus.OPEN,
                        Priority.HIGH,
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 1, 31),
                        "test search",
                        false,
                        SlaStatus.ACTIVE,
                        PageRequest.of(0, 10),
                        employeePrincipal
                )
        );

        assertEquals("Only Admin is allowed for this action", exception.getMessage());
        verify(ticketRepository, never()).findAllTicketsWithFilters(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("searchTickets with TicketView.ALL should succeed and call findAllTicketsWithFilters when role is ADMIN")
    void searchTickets_allView_admin_success() {
        Long adminId = 1L;
        Employee adminEmployee = new Employee();
        adminEmployee.setId(adminId);
        adminEmployee.setRole(UserRole.ADMIN);

        UserPrincipal adminPrincipal = new UserPrincipal(adminId, "admin@test.com", "", UserRole.ADMIN, "UTC", true, Collections.emptyList());

        when(employeeRepository.findById(adminId)).thenReturn(Optional.of(adminEmployee));

        Pageable pageable = PageRequest.of(0, 10);
        Ticket ticket = new Ticket();
        ticket.setId(500L);
        Page<Ticket> ticketPage = new PageImpl<>(List.of(ticket), pageable, 1);

        when(ticketRepository.findAllTicketsWithFilters(
                eq(TicketStatus.OPEN),
                eq(Priority.HIGH),
                any(),
                any(),
                eq("issue"),
                eq(false),
                eq(SlaStatus.ACTIVE),
                eq(pageable)
        )).thenReturn(ticketPage);

        when(slaInstanceRepository.findLatestByTicketIds(List.of(500L))).thenReturn(Collections.emptyList());
        TicketResponseDTO responseDTO = mock(TicketResponseDTO.class);
        when(ticketMapper.toResponse(eq(ticket), isNull())).thenReturn(responseDTO);

        Page<TicketResponseDTO> result = ticketService.searchTickets(
                TicketView.ALL,
                999L,
                888L,
                777L,
                TicketStatus.OPEN,
                Priority.HIGH,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31),
                "  issue  ",
                false,
                SlaStatus.ACTIVE,
                pageable,
                adminPrincipal
        );

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertSame(responseDTO, result.getContent().get(0));

        verify(ticketRepository).findAllTicketsWithFilters(
                eq(TicketStatus.OPEN),
                eq(Priority.HIGH),
                any(),
                any(),
                eq("issue"),
                eq(false),
                eq(SlaStatus.ACTIVE),
                eq(pageable)
        );
    }
}
