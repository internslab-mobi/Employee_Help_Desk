package xyz.mobi.employeehelpdesk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import xyz.mobi.employeehelpdesk.entity.DepartmentAgent;

import java.util.List;

public interface DepartmentAgentRepository
        extends JpaRepository<DepartmentAgent, Long> {


    /* Find all agents belonging to a particular department whose employee is currently ACTIVE and role is AGENT */
    @Query("""
        SELECT da
        FROM DepartmentAgent da
        JOIN FETCH da.employee e
        LEFT JOIN FETCH e.department
        WHERE e.department.id = :departmentId
          AND e.employmentStatus = xyz.mobi.employeehelpdesk.entity.enums.EmploymentStatus.ACTIVE
          AND e.role = xyz.mobi.employeehelpdesk.entity.enums.UserRole.AGENT
    """)
    List<DepartmentAgent> findEligibleAgents(
            @Param("departmentId") Long departmentId
    );
}