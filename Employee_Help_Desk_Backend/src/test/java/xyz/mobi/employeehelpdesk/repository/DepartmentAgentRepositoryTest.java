package xyz.mobi.employeehelpdesk.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import xyz.mobi.employeehelpdesk.entity.*;
import xyz.mobi.employeehelpdesk.entity.enums.EmploymentStatus;
import xyz.mobi.employeehelpdesk.entity.enums.Priority;
import xyz.mobi.employeehelpdesk.entity.enums.UserRole;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class DepartmentAgentRepositoryTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private DepartmentAgentRepository departmentAgentRepository;

    private Department deptIT;
    private Department deptHR;
    private Category catHardware;
    private Category catSoftware;
    private Category catPrinter;
    private Category catNetworking;
    private Category catSecurity;
    private Category catEmail;
    private SubCategory subCatLaptop;
    private Skill skillHardwareRepair;
    private DepartmentManager managerIT;

    @BeforeEach
    void setUp() {
        deptIT = new Department();
        deptIT.setCode("IT");
        deptIT.setName("Information Technology");
        entityManager.persist(deptIT);

        deptHR = new Department();
        deptHR.setCode("HR");
        deptHR.setName("Human Resources");
        entityManager.persist(deptHR);

        catHardware = createCategory(deptIT, "HARDWARE");
        catSoftware = createCategory(deptIT, "SOFTWARE");
        catPrinter = createCategory(deptIT, "PRINTER");
        catNetworking = createCategory(deptIT, "NETWORKING");
        catSecurity = createCategory(deptIT, "SECURITY");
        catEmail = createCategory(deptIT, "EMAIL");

        subCatLaptop = new SubCategory();
        subCatLaptop.setCategory(catHardware);
        subCatLaptop.setName("Laptop");
        subCatLaptop.setPriority(Priority.HIGH);
        entityManager.persist(subCatLaptop);

        skillHardwareRepair = new Skill();
        skillHardwareRepair.setName("Hardware Repair");
        entityManager.persist(skillHardwareRepair);

        SubCategorySkill scSkill = new SubCategorySkill();
        scSkill.setSubCategory(subCatLaptop);
        scSkill.setSkill(skillHardwareRepair);
        entityManager.persist(scSkill);

        // Manager employee
        Employee mgrEmp = new Employee();
        mgrEmp.setEmployeeCode("MGR001");
        mgrEmp.setFirstName("Manager");
        mgrEmp.setLastName("One");
        mgrEmp.setEmail("mgr1@example.com");
        mgrEmp.setDepartment(deptIT);
        mgrEmp.setRole(UserRole.MANAGER);
        mgrEmp.setEmploymentStatus(EmploymentStatus.ACTIVE);
        mgrEmp.setTimezone("UTC");
        mgrEmp.setDateOfJoining(LocalDate.now());
        entityManager.persist(mgrEmp);

        managerIT = new DepartmentManager();
        managerIT.setEmployee(mgrEmp);
        managerIT.setIsPrimary(true);
        entityManager.persist(managerIT);

        entityManager.flush();
    }

    private Category createCategory(Department dept, String name) {
        Category c = new Category();
        c.setDepartment(dept);
        c.setName(name);
        c.setIsActive(true);
        entityManager.persist(c);
        return c;
    }

    private DepartmentAgent createAgent(
            String code,
            String email,
            Department dept,
            EmploymentStatus status,
            UserRole role,
            DepartmentManager manager,
            Category... categories
    ) {
        Employee emp = new Employee();
        emp.setEmployeeCode(code);
        emp.setFirstName("First" + code);
        emp.setLastName("Last" + code);
        emp.setEmail(email);
        emp.setDepartment(dept);
        emp.setRole(role);
        emp.setEmploymentStatus(status);
        emp.setManager(manager);
        emp.setTimezone("UTC");
        emp.setDateOfJoining(LocalDate.now());
        entityManager.persist(emp);

        DepartmentAgent da = new DepartmentAgent();
        da.setEmployee(emp);
        if (categories != null) {
            for (Category c : categories) {
                if (c != null) {
                    da.addCategory(c);
                }
            }
        }
        entityManager.persist(da);
        return da;
    }

    private void assignSkill(DepartmentAgent agent, Skill skill) {
        AgentSkill as = new AgentSkill();
        as.setAgent(agent);
        as.setSkill(skill);
        entityManager.persist(as);
    }

    @Test
    @DisplayName("Scenario 1, 2, 3: Multiple categories per agent, multiple agents per category, queried by each category")
    void testMultipleCategoriesAndMultipleAgents() {
        // Bob: HARDWARE, SOFTWARE, PRINTER
        DepartmentAgent bob = createAgent(
                "BOB001", "bob@example.com", deptIT,
                EmploymentStatus.ACTIVE, UserRole.AGENT, managerIT,
                catHardware, catSoftware, catPrinter
        );

        // Carol: NETWORKING, SECURITY
        DepartmentAgent carol = createAgent(
                "CAR001", "carol@example.com", deptIT,
                EmploymentStatus.ACTIVE, UserRole.AGENT, managerIT,
                catNetworking, catSecurity
        );

        // David: SOFTWARE, EMAIL
        DepartmentAgent david = createAgent(
                "DAV001", "david@example.com", deptIT,
                EmploymentStatus.ACTIVE, UserRole.AGENT, managerIT,
                catSoftware, catEmail
        );

        entityManager.flush();
        entityManager.clear();

        // Query HARDWARE -> only Bob
        List<DepartmentAgent> hardwareAgents = departmentAgentRepository.findEligibleAgents(
                deptIT.getId(), catHardware.getId()
        );
        assertEquals(1, hardwareAgents.size());
        assertEquals(bob.getId(), hardwareAgents.get(0).getId());

        // Query SOFTWARE -> Bob and David (multiple agents for SOFTWARE)
        List<DepartmentAgent> softwareAgents = departmentAgentRepository.findEligibleAgents(
                deptIT.getId(), catSoftware.getId()
        );
        assertEquals(2, softwareAgents.size());
        List<Long> softwareAgentIds = softwareAgents.stream().map(DepartmentAgent::getId).toList();
        assertTrue(softwareAgentIds.contains(bob.getId()));
        assertTrue(softwareAgentIds.contains(david.getId()));
        assertFalse(softwareAgentIds.contains(carol.getId()));

        // Query PRINTER -> only Bob
        List<DepartmentAgent> printerAgents = departmentAgentRepository.findEligibleAgents(
                deptIT.getId(), catPrinter.getId()
        );
        assertEquals(1, printerAgents.size());
        assertEquals(bob.getId(), printerAgents.get(0).getId());

        // Query NETWORKING -> only Carol
        List<DepartmentAgent> networkingAgents = departmentAgentRepository.findEligibleAgents(
                deptIT.getId(), catNetworking.getId()
        );
        assertEquals(1, networkingAgents.size());
        assertEquals(carol.getId(), networkingAgents.get(0).getId());

        // Query SECURITY -> only Carol
        List<DepartmentAgent> securityAgents = departmentAgentRepository.findEligibleAgents(
                deptIT.getId(), catSecurity.getId()
        );
        assertEquals(1, securityAgents.size());
        assertEquals(carol.getId(), securityAgents.get(0).getId());

        // Query EMAIL -> only David
        List<DepartmentAgent> emailAgents = departmentAgentRepository.findEligibleAgents(
                deptIT.getId(), catEmail.getId()
        );
        assertEquals(1, emailAgents.size());
        assertEquals(david.getId(), emailAgents.get(0).getId());
    }

    @Test
    @DisplayName("Scenario 4: Agent excluded when not associated with requested category")
    void testAgentExcludedWhenNotAssociatedWithRequestedCategory() {
        // Carol: only NETWORKING, SECURITY
        createAgent(
                "CAR002", "carol2@example.com", deptIT,
                EmploymentStatus.ACTIVE, UserRole.AGENT, managerIT,
                catNetworking, catSecurity
        );

        // David: only SOFTWARE, EMAIL
        createAgent(
                "DAV002", "david2@example.com", deptIT,
                EmploymentStatus.ACTIVE, UserRole.AGENT, managerIT,
                catSoftware, catEmail
        );

        entityManager.flush();
        entityManager.clear();

        // Query HARDWARE -> Neither Carol nor David should match
        List<DepartmentAgent> result = departmentAgentRepository.findEligibleAgents(
                deptIT.getId(), catHardware.getId()
        );
        assertTrue(result.isEmpty(), "No agent in HARDWARE should be returned");
    }

    @Test
    @DisplayName("Scenario 5: Inactive employees and non-AGENT roles are excluded")
    void testInactiveAndNonAgentRolesExcluded() {
        // Inactive agent with HARDWARE
        createAgent(
                "INA001", "inactive@example.com", deptIT,
                EmploymentStatus.INACTIVE, UserRole.AGENT, managerIT,
                catHardware
        );

        // Employee with role EMPLOYEE with HARDWARE
        createAgent(
                "EMP001", "employee@example.com", deptIT,
                EmploymentStatus.ACTIVE, UserRole.EMPLOYEE, managerIT,
                catHardware
        );

        // Employee with role ADMIN with HARDWARE
        createAgent(
                "ADM001", "admin@example.com", deptIT,
                EmploymentStatus.ACTIVE, UserRole.ADMIN, managerIT,
                catHardware
        );

        // Active AGENT with HARDWARE
        DepartmentAgent activeAgent = createAgent(
                "AGT001", "active@example.com", deptIT,
                EmploymentStatus.ACTIVE, UserRole.AGENT, managerIT,
                catHardware
        );

        entityManager.flush();
        entityManager.clear();

        List<DepartmentAgent> result = departmentAgentRepository.findEligibleAgents(
                deptIT.getId(), catHardware.getId()
        );

        assertEquals(1, result.size());
        assertEquals(activeAgent.getId(), result.get(0).getId());
    }

    @Test
    @DisplayName("Scenario 6: Department filtering works correctly")
    void testDepartmentFiltering() {
        // Agent in HR department with HARDWARE
        createAgent(
                "HR001", "hragent@example.com", deptHR,
                EmploymentStatus.ACTIVE, UserRole.AGENT, null,
                catHardware
        );

        // Agent in IT department with HARDWARE
        DepartmentAgent itAgent = createAgent(
                "IT001", "itagent@example.com", deptIT,
                EmploymentStatus.ACTIVE, UserRole.AGENT, managerIT,
                catHardware
        );

        entityManager.flush();
        entityManager.clear();

        // Query IT department
        List<DepartmentAgent> itResults = departmentAgentRepository.findEligibleAgents(
                deptIT.getId(), catHardware.getId()
        );
        assertEquals(1, itResults.size());
        assertEquals(itAgent.getId(), itResults.get(0).getId());

        // Query HR department
        List<DepartmentAgent> hrResults = departmentAgentRepository.findEligibleAgents(
                deptHR.getId(), catHardware.getId()
        );
        assertEquals(1, hrResults.size());
        assertNotEquals(itAgent.getId(), hrResults.get(0).getId());
    }

    @Test
    @DisplayName("Scenario 7: No duplicate agent results occur when querying with null category or multiple categories")
    void testNoDuplicateAgentResults() {
        // Agent with multiple categories
        DepartmentAgent multiCatAgent = createAgent(
                "MUL001", "multi@example.com", deptIT,
                EmploymentStatus.ACTIVE, UserRole.AGENT, managerIT,
                catHardware, catSoftware, catPrinter
        );

        entityManager.flush();
        entityManager.clear();

        // Query with null categoryId (returns all active department agents)
        List<DepartmentAgent> result = departmentAgentRepository.findEligibleAgents(
                deptIT.getId(), null
        );

        assertEquals(1, result.size(), "Agent should appear exactly once despite having 3 categories");
        assertEquals(multiCatAgent.getId(), result.get(0).getId());
    }

    @Test
    @DisplayName("Scenario 8: Agent with optional or missing manager relationship is returned")
    void testAgentWithMissingManagerIsReturned() {
        DepartmentAgent agentNoManager = createAgent(
                "NOMGR1", "nomanager@example.com", deptIT,
                EmploymentStatus.ACTIVE, UserRole.AGENT, null,
                catHardware
        );

        entityManager.flush();
        entityManager.clear();

        List<DepartmentAgent> result = departmentAgentRepository.findEligibleAgents(
                deptIT.getId(), catHardware.getId()
        );

        assertEquals(1, result.size());
        assertEquals(agentNoManager.getId(), result.get(0).getId());
        assertNull(result.get(0).getEmployee().getManager(), "Manager relationship is properly null");
    }

    @Test
    @DisplayName("Scenario 9: Agent-category assignments can be created, updated, and removed correctly")
    void testAgentCategoryCreateUpdateRemove() {
        DepartmentAgent agent = createAgent(
                "CRU001", "crud@example.com", deptIT,
                EmploymentStatus.ACTIVE, UserRole.AGENT, managerIT,
                catHardware
        );

        entityManager.flush();
        entityManager.clear();

        // Verify initial creation
        DepartmentAgent loaded = departmentAgentRepository.findById(agent.getId()).orElseThrow();
        assertEquals(1, loaded.getCategories().size());
        assertTrue(loaded.getCategories().stream().anyMatch(c -> c.getName().equals("HARDWARE")));

        // Update: add SOFTWARE and PRINTER
        loaded.addCategory(catSoftware);
        loaded.addCategory(catPrinter);
        departmentAgentRepository.save(loaded);
        entityManager.flush();
        entityManager.clear();

        loaded = departmentAgentRepository.findById(agent.getId()).orElseThrow();
        assertEquals(3, loaded.getCategories().size());

        // Remove: remove HARDWARE
        Category hardwareToRem = entityManager.find(Category.class, catHardware.getId());
        loaded.removeCategory(hardwareToRem);
        departmentAgentRepository.save(loaded);
        entityManager.flush();
        entityManager.clear();

        loaded = departmentAgentRepository.findById(agent.getId()).orElseThrow();
        assertEquals(2, loaded.getCategories().size());
        assertFalse(loaded.getCategories().stream().anyMatch(c -> c.getName().equals("HARDWARE")));
        assertTrue(loaded.getCategories().stream().anyMatch(c -> c.getName().equals("SOFTWARE")));
        assertTrue(loaded.getCategories().stream().anyMatch(c -> c.getName().equals("PRINTER")));
    }

    @Test
    @DisplayName("Scenario 10: Existing skill eligibility and lastAssignedAt workload behavior preserved")
    void testSkillEligibilityAndWorkloadPreserved() {
        DepartmentAgent agentWithSkill = createAgent(
                "SKL001", "skill@example.com", deptIT,
                EmploymentStatus.ACTIVE, UserRole.AGENT, managerIT,
                catHardware
        );
        assignSkill(agentWithSkill, skillHardwareRepair);

        Instant assignTime = Instant.now();
        agentWithSkill.setLastAssignedAt(assignTime);
        departmentAgentRepository.save(agentWithSkill);

        entityManager.flush();
        entityManager.clear();

        List<DepartmentAgent> result = departmentAgentRepository.findEligibleAgents(
                deptIT.getId(), catHardware.getId()
        );

        assertEquals(1, result.size());
        assertEquals(agentWithSkill.getId(), result.get(0).getId());
        assertNotNull(result.get(0).getLastAssignedAt());
        assertEquals(assignTime.getEpochSecond(), result.get(0).getLastAssignedAt().getEpochSecond());
    }
}
