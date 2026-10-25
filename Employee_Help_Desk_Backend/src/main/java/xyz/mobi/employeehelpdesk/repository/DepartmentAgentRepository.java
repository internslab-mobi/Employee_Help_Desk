package xyz.mobi.employeehelpdesk.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import xyz.mobi.employeehelpdesk.entity.DepartmentAgent;
import xyz.mobi.employeehelpdesk.entity.enums.UserRole;
import xyz.mobi.employeehelpdesk.entity.enums.EmploymentStatus;

import java.util.List;
import java.util.Optional;

public interface DepartmentAgentRepository
        extends JpaRepository<DepartmentAgent, Long> {

    @Query("""
        SELECT da
        FROM DepartmentAgent da
        JOIN FETCH da.employee e
        LEFT JOIN FETCH e.department
        LEFT JOIN FETCH e.manager
        WHERE e.department.id = :departmentId
    """)
    List<DepartmentAgent> findByDepartmentId(@Param("departmentId") Long departmentId);

    @EntityGraph(attributePaths = {"employee", "employee.department", "employee.manager"})
    Optional<DepartmentAgent> findByEmployeeId(Long employeeId);

    @Query("""
        SELECT da
        FROM DepartmentAgent da
        JOIN FETCH da.employee e
        LEFT JOIN FETCH e.department
        LEFT JOIN FETCH e.manager
        WHERE e.department.id = :departmentId
          AND e.id = :employeeId
    """)
    Optional<DepartmentAgent> findByDepartmentIdAndEmployeeId(
            @Param("departmentId") Long departmentId,
            @Param("employeeId") Long employeeId
    );

    @Query("""
        SELECT COUNT(da) > 0
        FROM DepartmentAgent da
        WHERE da.employee.id = :employeeId
          AND da.employee.department.id = :departmentId
    """)
    Boolean existsByEmployeeIdAndDepartmentId(
            @Param("employeeId") Long employeeId,
            @Param("departmentId") Long departmentId
    );

    /* Find all agents belonging to a particular department whose employee is currently ACTIVE and role is AGENT */
    @Query("""
        SELECT DISTINCT da
        FROM DepartmentAgent da
        JOIN FETCH da.employee e
        LEFT JOIN FETCH e.department
        LEFT JOIN FETCH e.manager
        WHERE e.department.id = :departmentId
          AND e.employmentStatus = EmploymentStatus.ACTIVE
          AND e.role = UserRole.AGENT
    """)
    List<DepartmentAgent> findEligibleAgents(
            @Param("departmentId") Long departmentId
    );

    default List<DepartmentAgent> findEligibleAgents(Long departmentId, Long categoryId) {
        return findEligibleAgents(departmentId);
    }
}