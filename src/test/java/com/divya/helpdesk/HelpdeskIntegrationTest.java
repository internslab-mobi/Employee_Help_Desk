package com.divya.helpdesk;

import com.divya.helpdesk.controller.*;
import com.divya.helpdesk.dto.request.*;
import com.divya.helpdesk.dto.response.*;
import com.divya.helpdesk.enums.HDEmploymentStatus;
import com.divya.helpdesk.enums.HDPriorityLevel;
import com.divya.helpdesk.enums.HDTicketStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class HelpdeskIntegrationTest {

    @Autowired
    private BusinessCalendarController calendarController;

    @Autowired
    private DepartmentController departmentController;

    @Autowired
    private CategoryController categoryController;

    @Autowired
    private SkillController skillController;

    @Autowired
    private SlaPolicyController slaPolicyController;

    @Autowired
    private TicketController ticketController;

    @Test
    void testEndToEndControllerFlow() {
        // 1. Create Business Calendar
        BusinessCalendarRequest calReq = new BusinessCalendarRequest("STD_CAL", "Standard 9-5", "Desc", "UTC", true, true);
        ResponseEntity<BusinessCalendarResponse> calRes = calendarController.createCalendar(calReq);
        assertEquals(HttpStatus.CREATED, calRes.getStatusCode());
        assertNotNull(calRes.getBody());
        Long calendarId = calRes.getBody().getId();

        // 2. Add Business Hours
        BusinessHoursRequest bhReq = new BusinessHoursRequest(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(17, 0), true);
        ResponseEntity<BusinessHoursResponse> bhRes = calendarController.createBusinessHours(calendarId, bhReq);
        assertEquals(HttpStatus.CREATED, bhRes.getStatusCode());

        // 3. Add Holiday
        HolidayRequest holReq = new HolidayRequest(LocalDate.of(2026, 12, 25), "Christmas", "Holiday");
        ResponseEntity<HolidayResponse> holRes = calendarController.createHoliday(calendarId, holReq);
        assertEquals(HttpStatus.CREATED, holRes.getStatusCode());

        // 4. Create Department
        DepartmentCreateRequest deptReq = new DepartmentCreateRequest("IT", "Information Tech", "IT Department", calendarId, true);
        ResponseEntity<DepartmentResponse> deptRes = departmentController.createDepartment(deptReq);
        assertEquals(HttpStatus.CREATED, deptRes.getStatusCode());
        assertNotNull(deptRes.getBody());
        Long deptId = deptRes.getBody().getId();

        // 5. Create Employees (Requester, Agent, Manager)
        EmployeeCreateRequest empReq = new EmployeeCreateRequest("EMP101", "Dave", "User", "dave@corp.com", "555-1234", "Developer", deptId, HDEmploymentStatus.ACTIVE, LocalDate.now(), null);
        ResponseEntity<EmployeeResponse> reqEmpRes = departmentController.createEmployee(empReq);
        assertEquals(HttpStatus.CREATED, reqEmpRes.getStatusCode());
        assertNotNull(reqEmpRes.getBody());
        Long requesterId = reqEmpRes.getBody().getId();

        EmployeeCreateRequest agentEmpReq = new EmployeeCreateRequest("EMP102", "Agent", "Smith", "agent.smith@corp.com", "555-5678", "Support Agent", deptId, HDEmploymentStatus.ACTIVE, LocalDate.now(), null);
        ResponseEntity<EmployeeResponse> agentEmpRes = departmentController.createEmployee(agentEmpReq);
        assertEquals(HttpStatus.CREATED, agentEmpRes.getStatusCode());
        assertNotNull(agentEmpRes.getBody());
        Long agentEmpId = agentEmpRes.getBody().getId();

        EmployeeCreateRequest mgrEmpReq = new EmployeeCreateRequest("EMP103", "Mary", "Boss", "mary@corp.com", "555-9999", "IT Manager", deptId, HDEmploymentStatus.ACTIVE, LocalDate.now(), null);
        ResponseEntity<EmployeeResponse> mgrEmpRes = departmentController.createEmployee(mgrEmpReq);
        assertEquals(HttpStatus.CREATED, mgrEmpRes.getStatusCode());
        assertNotNull(mgrEmpRes.getBody());
        Long mgrEmpId = mgrEmpRes.getBody().getId();

        // 6. Profile Image Upload & Download
        MockMultipartFile imageFile = new MockMultipartFile("file", "avatar.png", "image/png", "dummy-image-bytes".getBytes());
        ResponseEntity<String> uploadRes = departmentController.uploadProfileImage(requesterId, imageFile);
        assertEquals(HttpStatus.OK, uploadRes.getStatusCode());

        ResponseEntity<byte[]> imgRes = departmentController.getProfileImage(requesterId);
        assertEquals(HttpStatus.OK, imgRes.getStatusCode());
        assertArrayEquals("dummy-image-bytes".getBytes(), imgRes.getBody());

        // 7. Add Agent & Manager to Department
        ResponseEntity<DepartmentAgentResponse> agentRes = departmentController.addAgent(deptId, new DepartmentAgentRequest(agentEmpId));
        assertEquals(HttpStatus.CREATED, agentRes.getStatusCode());
        assertNotNull(agentRes.getBody());
        Long agentId = agentRes.getBody().getId();

        ResponseEntity<DepartmentManagerResponse> mgrRes = departmentController.addManager(deptId, new DepartmentManagerRequest(mgrEmpId, true));
        assertEquals(HttpStatus.CREATED, mgrRes.getStatusCode());
        assertTrue(mgrRes.getBody().getIsPrimary());

        // 8. Create Skill and Assign to Agent
        ResponseEntity<SkillResponse> skillRes = skillController.createSkill(new SkillCreateRequest("Networking", "Network admin", true));
        assertEquals(HttpStatus.CREATED, skillRes.getStatusCode());
        assertNotNull(skillRes.getBody());
        Long skillId = skillRes.getBody().getId();

        ResponseEntity<AgentSkillResponse> agSkillRes = skillController.assignSkillToAgent(new AgentSkillAssignRequest(agentId, skillId));
        assertEquals(HttpStatus.CREATED, agSkillRes.getStatusCode());

        // 9. Create Category and SubCategory
        ResponseEntity<CategoryResponse> catRes = categoryController.createCategory(new CategoryCreateRequest(deptId, "Hardware & Network", "Desc", requesterId, true));
        assertEquals(HttpStatus.CREATED, catRes.getStatusCode());
        assertNotNull(catRes.getBody());
        Long catId = catRes.getBody().getId();

        ResponseEntity<SubCategoryResponse> subCatRes = categoryController.createSubCategory(new SubCategoryCreateRequest(catId, "VPN Connectivity", "VPN issue", requesterId, HDPriorityLevel.HIGH, true));
        assertEquals(HttpStatus.CREATED, subCatRes.getStatusCode());
        assertNotNull(subCatRes.getBody());
        Long subCatId = subCatRes.getBody().getId();

        // Assign skill to subcategory
        ResponseEntity<SubCategorySkillResponse> scSkillRes = skillController.assignSkillToSubCategory(new SubCategorySkillAssignRequest(subCatId, skillId));
        assertEquals(HttpStatus.CREATED, scSkillRes.getStatusCode());

        // 10. Create SLA Policy
        ResponseEntity<SlaPolicyResponse> slaRes = slaPolicyController.createOrUpdatePolicy(new SlaPolicyRequest(deptId, subCatId, 240, 180, true));
        assertEquals(HttpStatus.OK, slaRes.getStatusCode());
        assertNotNull(slaRes.getBody());
        assertEquals(240, slaRes.getBody().getDurationMinutes());
        assertEquals(180, slaRes.getBody().getWarningMinutes());

        // 11. Create Ticket (Auto assigned to agent, SLA calculated)
        TicketCreateRequest ticketCreateReq = new TicketCreateRequest(
                requesterId, deptId, catId, subCatId, "Cannot connect to VPN", "Cisco AnyConnect timeout"
        );

        ResponseEntity<TicketResponse> ticketRes = ticketController.createTicket(ticketCreateReq);
        assertEquals(HttpStatus.CREATED, ticketRes.getStatusCode());
        assertNotNull(ticketRes.getBody());
        TicketResponse ticket = ticketRes.getBody();
        assertNotNull(ticket.getTicketNumber());
        assertEquals(HDTicketStatus.ASSIGNED, ticket.getStatus());
        assertEquals(HDPriorityLevel.HIGH, ticket.getPriority()); // Priority assigned from subcategory
        assertNotNull(ticket.getDueAt()); // SLA deadline is calculated and exposed on ticket response
        assertEquals(agentEmpId, ticket.getAssignedAgentId());
        Long ticketId = ticket.getId();

        // 12. Query Ticket by ID, Ticket Number, and Details
        ResponseEntity<TicketResponse> getTicketRes = ticketController.getTicketById(ticketId);
        assertEquals(HttpStatus.OK, getTicketRes.getStatusCode());
        assertEquals(ticketId, getTicketRes.getBody().getId());

        ResponseEntity<TicketResponse> getTicketByNumRes = ticketController.getTicketByNumber(ticket.getTicketNumber());
        assertEquals(HttpStatus.OK, getTicketByNumRes.getStatusCode());
        assertEquals(ticket.getTicketNumber(), getTicketByNumRes.getBody().getTicketNumber());

        ResponseEntity<TicketDetailsResponse> detailsRes = ticketController.getTicketDetails(ticketId);
        assertEquals(HttpStatus.OK, detailsRes.getStatusCode());
        assertNotNull(detailsRes.getBody());
        assertEquals("dave@corp.com", detailsRes.getBody().getRequester().getEmail());
        assertEquals("agent.smith@corp.com", detailsRes.getBody().getAssignedAgent().getEmail());
        assertNotNull(detailsRes.getBody().getSlaInstance());
        assertEquals(240, detailsRes.getBody().getSlaInstance().getAllocatedMinutes());
        assertNotNull(detailsRes.getBody().getSlaInstance().getCurrentDeadlineAt());
        assertNotNull(detailsRes.getBody().getSlaInstance().getWarningAt());

        // 13. Filter Tickets
        ResponseEntity<List<TicketResponse>> filteredRes = ticketController.getTickets(requesterId, deptId, agentEmpId, HDTicketStatus.ASSIGNED, HDPriorityLevel.HIGH);
        assertEquals(HttpStatus.OK, filteredRes.getStatusCode());
        assertEquals(1, filteredRes.getBody().size());

        // 14. Start Working on Ticket (ASSIGNED -> IN_PROGRESS)
        ResponseEntity<TicketResponse> startWorkRes = ticketController.startWorkingOnTicket(ticketId);
        assertEquals(HttpStatus.OK, startWorkRes.getStatusCode());
        assertEquals(HDTicketStatus.IN_PROGRESS, startWorkRes.getBody().getStatus());

        // 15. Resolve Ticket (IN_PROGRESS -> RESOLVED)
        TicketResolveRequest resolveReq = new TicketResolveRequest("Reconfigured VPN profile settings");
        ResponseEntity<TicketResponse> resolveRes = ticketController.resolveTicket(ticketId, resolveReq);
        assertEquals(HttpStatus.OK, resolveRes.getStatusCode());
        assertEquals(HDTicketStatus.RESOLVED, resolveRes.getBody().getStatus());
        assertNotNull(resolveRes.getBody().getResolvedAt());
        assertEquals("Reconfigured VPN profile settings", resolveRes.getBody().getResolutionSummary());
    }
}
