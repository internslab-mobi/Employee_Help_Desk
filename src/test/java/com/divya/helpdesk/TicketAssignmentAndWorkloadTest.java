package com.divya.helpdesk;

import com.divya.helpdesk.dto.request.*;
import com.divya.helpdesk.dto.response.*;
import com.divya.helpdesk.enums.HDEmploymentStatus;
import com.divya.helpdesk.enums.HDPriorityLevel;
import com.divya.helpdesk.enums.HDTicketStatus;
import com.divya.helpdesk.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class TicketAssignmentAndWorkloadTest {

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private SubCategoryService subCategoryService;

    @Autowired
    private SkillService skillService;

    @Autowired
    private SlaPolicyService slaPolicyService;

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketAssignmentService ticketAssignmentService;

    private DepartmentResponse department;
    private EmployeeResponse requester;
    private EmployeeResponse agentEmp1;
    private EmployeeResponse agentEmp2;
    private EmployeeResponse managerEmp;
    private DepartmentAgentResponse agent1;
    private DepartmentAgentResponse agent2;
    private DepartmentManagerResponse manager;
    private SkillResponse javaSkill;
    private SkillResponse sqlSkill;
    private CategoryResponse category;
    private SubCategoryResponse subCategoryMedium;
    private SubCategoryResponse subCategoryHigh;
    private SubCategoryResponse subCategoryCritical;
    private SubCategoryResponse subCategoryLow;

    @BeforeEach
    void setUp() {
        // 1. Department
        DepartmentCreateRequest deptReq = new DepartmentCreateRequest();
        deptReq.setCode("ENG");
        deptReq.setName("Engineering");
        department = departmentService.createDepartment(deptReq);

        // 2. Employees
        EmployeeCreateRequest reqEmp = new EmployeeCreateRequest("EMP001", "John", "Requester", "requester@test.com", "12345", "Engineer", department.getId(), HDEmploymentStatus.ACTIVE, LocalDate.now(), null);
        requester = employeeService.createEmployee(reqEmp);

        EmployeeCreateRequest agEmp1Req = new EmployeeCreateRequest("EMP002", "Alice", "Agent", "alice@test.com", "12346", "Support Agent", department.getId(), HDEmploymentStatus.ACTIVE, LocalDate.now(), null);
        agentEmp1 = employeeService.createEmployee(agEmp1Req);

        EmployeeCreateRequest agEmp2Req = new EmployeeCreateRequest("EMP003", "Bob", "Agent", "bob@test.com", "12347", "Support Agent", department.getId(), HDEmploymentStatus.ACTIVE, LocalDate.now(), null);
        agentEmp2 = employeeService.createEmployee(agEmp2Req);

        EmployeeCreateRequest mgrEmpReq = new EmployeeCreateRequest("EMP004", "Charlie", "Manager", "charlie@test.com", "12348", "Engineering Manager", department.getId(), HDEmploymentStatus.ACTIVE, LocalDate.now(), null);
        managerEmp = employeeService.createEmployee(mgrEmpReq);

        // 3. Register Agents and Manager
        agent1 = departmentService.addAgent(department.getId(), new DepartmentAgentRequest(agentEmp1.getId()));
        agent2 = departmentService.addAgent(department.getId(), new DepartmentAgentRequest(agentEmp2.getId()));
        manager = departmentService.addManager(department.getId(), new DepartmentManagerRequest(managerEmp.getId(), true));

        // 4. Skills
        javaSkill = skillService.createSkill(new SkillCreateRequest("Java", "Java Programming", true));
        sqlSkill = skillService.createSkill(new SkillCreateRequest("SQL", "Database SQL", true));

        // 5. Category & SubCategories with various DB-configured priorities
        category = categoryService.createCategory(new CategoryCreateRequest(department.getId(), "Backend Support", "Backend queries", requester.getId(), true));
        
        subCategoryMedium = subCategoryService.createSubCategory(new SubCategoryCreateRequest(category.getId(), "Java Exceptions", "Java runtime bugs", requester.getId(), HDPriorityLevel.MEDIUM, true));
        subCategoryHigh = subCategoryService.createSubCategory(new SubCategoryCreateRequest(category.getId(), "Java OutOfMemory", "High memory usage", requester.getId(), HDPriorityLevel.HIGH, true));
        subCategoryCritical = subCategoryService.createSubCategory(new SubCategoryCreateRequest(category.getId(), "System Crash", "Kernel panic", requester.getId(), HDPriorityLevel.CRITICAL, true));
        subCategoryLow = subCategoryService.createSubCategory(new SubCategoryCreateRequest(category.getId(), "Code Cleanup", "Minor refactoring", requester.getId(), HDPriorityLevel.LOW, true));

        // Subcategories require Java skill
        skillService.assignSkillToSubCategory(new SubCategorySkillAssignRequest(subCategoryMedium.getId(), javaSkill.getId()));
        skillService.assignSkillToSubCategory(new SubCategorySkillAssignRequest(subCategoryHigh.getId(), javaSkill.getId()));
        skillService.assignSkillToSubCategory(new SubCategorySkillAssignRequest(subCategoryCritical.getId(), javaSkill.getId()));
        skillService.assignSkillToSubCategory(new SubCategorySkillAssignRequest(subCategoryLow.getId(), javaSkill.getId()));
    }

    @Test
    void testPriorityWeightValues() {
        assertEquals(1, HDPriorityLevel.LOW.getWeight());
        assertEquals(2, HDPriorityLevel.MEDIUM.getWeight());
        assertEquals(3, HDPriorityLevel.HIGH.getWeight());
        assertEquals(4, HDPriorityLevel.CRITICAL.getWeight());
    }

    @Test
    void testSkillMatchingAssignment() {
        // Agent 1 has Java skill, Agent 2 has SQL skill
        skillService.assignSkillToAgent(new AgentSkillAssignRequest(agent1.getId(), javaSkill.getId()));
        skillService.assignSkillToAgent(new AgentSkillAssignRequest(agent2.getId(), sqlSkill.getId()));

        TicketCreateRequest ticketReq = new TicketCreateRequest(
                requester.getId(),
                department.getId(),
                category.getId(),
                subCategoryHigh.getId(),
                "NullPointerException in Auth",
                "Stacktrace details"
        );

        TicketResponse ticket = ticketService.createTicket(ticketReq);

        // Priority is automatically set to HIGH from subcategory in DB
        assertEquals(HDPriorityLevel.HIGH, ticket.getPriority());

        // Ticket requires Java -> Agent 1 (Alice) must be assigned
        assertNotNull(ticket.getAssignedAgentId());
        assertEquals(agentEmp1.getId(), ticket.getAssignedAgentId());
        assertEquals(HDTicketStatus.ASSIGNED, ticket.getStatus());
        assertNull(ticket.getAssignedManagerId());

        // Alice workload should now be 3 (HIGH priority = 3)
        assertEquals(3, ticketAssignmentService.calculateAgentWorkload(agentEmp1.getId()));
        // Bob workload should be 0
        assertEquals(0, ticketAssignmentService.calculateAgentWorkload(agentEmp2.getId()));
    }

    @Test
    void testLowestWorkloadAssignment() {
        // Both agents have Java skill
        skillService.assignSkillToAgent(new AgentSkillAssignRequest(agent1.getId(), javaSkill.getId()));
        skillService.assignSkillToAgent(new AgentSkillAssignRequest(agent2.getId(), javaSkill.getId()));

        // Create 1st ticket -> assigned to Agent 1 (Alice, min empId 2 vs 3, priority MEDIUM=2)
        TicketCreateRequest t1 = new TicketCreateRequest(requester.getId(), department.getId(), category.getId(), subCategoryMedium.getId(), "Issue 1", "Desc 1");
        TicketResponse ticket1 = ticketService.createTicket(t1);
        assertEquals(agentEmp1.getId(), ticket1.getAssignedAgentId());
        assertEquals(HDPriorityLevel.MEDIUM, ticket1.getPriority());

        // Agent 1 workload = 2, Agent 2 workload = 0
        assertEquals(2, ticketAssignmentService.calculateAgentWorkload(agentEmp1.getId()));
        assertEquals(0, ticketAssignmentService.calculateAgentWorkload(agentEmp2.getId()));

        // Create 2nd ticket -> must be assigned to Agent 2 (Bob) because Bob has lower workload (0 < 2, priority CRITICAL=4)
        TicketCreateRequest t2 = new TicketCreateRequest(requester.getId(), department.getId(), category.getId(), subCategoryCritical.getId(), "Issue 2", "Desc 2");
        TicketResponse ticket2 = ticketService.createTicket(t2);
        assertEquals(agentEmp2.getId(), ticket2.getAssignedAgentId());
        assertEquals(HDPriorityLevel.CRITICAL, ticket2.getPriority());

        // Agent 1 workload = 2, Agent 2 workload = 4
        assertEquals(2, ticketAssignmentService.calculateAgentWorkload(agentEmp1.getId()));
        assertEquals(4, ticketAssignmentService.calculateAgentWorkload(agentEmp2.getId()));

        // Create 3rd ticket -> must be assigned to Agent 1 (Alice) because 2 < 4 (priority LOW=1)
        TicketCreateRequest t3 = new TicketCreateRequest(requester.getId(), department.getId(), category.getId(), subCategoryLow.getId(), "Issue 3", "Desc 3");
        TicketResponse ticket3 = ticketService.createTicket(t3);
        assertEquals(agentEmp1.getId(), ticket3.getAssignedAgentId());
        assertEquals(HDPriorityLevel.LOW, ticket3.getPriority());

        // Agent 1 workload = 2 + 1 = 3, Agent 2 workload = 4
        assertEquals(3, ticketAssignmentService.calculateAgentWorkload(agentEmp1.getId()));
        assertEquals(4, ticketAssignmentService.calculateAgentWorkload(agentEmp2.getId()));
    }

    @Test
    void testManagerEscalationWhenNoMatchingAgent() {
        // Neither agent has the required Java skill (Agent 1 has SQL only)
        skillService.assignSkillToAgent(new AgentSkillAssignRequest(agent1.getId(), sqlSkill.getId()));

        TicketCreateRequest ticketReq = new TicketCreateRequest(
                requester.getId(),
                department.getId(),
                category.getId(),
                subCategoryHigh.getId(),
                "Unmatched Skill Issue",
                "Needs Java expert"
        );

        TicketResponse ticket = ticketService.createTicket(ticketReq);

        // Escalated to Department Manager
        assertNull(ticket.getAssignedAgentId());
        assertNotNull(ticket.getAssignedManagerId());
        assertEquals(managerEmp.getId(), ticket.getAssignedManagerId());
    }

    @Test
    void testTicketLifecycleStatusAndWorkloadReduction() {
        skillService.assignSkillToAgent(new AgentSkillAssignRequest(agent1.getId(), javaSkill.getId()));

        TicketCreateRequest ticketReq = new TicketCreateRequest(
                requester.getId(),
                department.getId(),
                category.getId(),
                subCategoryHigh.getId(), // weight = 3
                "Lifecycle Test Ticket",
                "Desc"
        );

        TicketResponse createdTicket = ticketService.createTicket(ticketReq);
        assertEquals(HDTicketStatus.ASSIGNED, createdTicket.getStatus());
        assertEquals(HDPriorityLevel.HIGH, createdTicket.getPriority());
        assertEquals(3, ticketAssignmentService.calculateAgentWorkload(agentEmp1.getId()));

        // Start working on ticket -> status IN_PROGRESS
        TicketResponse inProgressTicket = ticketService.startWorkingOnTicket(createdTicket.getId());
        assertEquals(HDTicketStatus.IN_PROGRESS, inProgressTicket.getStatus());
        // Workload is still active (3)
        assertEquals(3, ticketAssignmentService.calculateAgentWorkload(agentEmp1.getId()));

        // Resolve ticket -> status RESOLVED
        TicketResolveRequest resolveReq = new TicketResolveRequest("Fixed by applying hotfix patch");
        TicketResponse resolvedTicket = ticketService.resolveTicket(createdTicket.getId(), resolveReq);
        assertEquals(HDTicketStatus.RESOLVED, resolvedTicket.getStatus());
        assertNotNull(resolvedTicket.getResolvedAt());
        assertEquals("Fixed by applying hotfix patch", resolvedTicket.getResolutionSummary());

        // Agent 1 workload should now be 0 because ticket is resolved!
        assertEquals(0, ticketAssignmentService.calculateAgentWorkload(agentEmp1.getId()));
    }
}
