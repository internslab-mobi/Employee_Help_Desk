package xyz.mobi.employeehelpdesk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import xyz.mobi.employeehelpdesk.entity.DepartmentAgent;

import java.util.List;

public interface DepartmentAgentRepository
        extends JpaRepository<DepartmentAgent, Long> {

    List<DepartmentAgent> findByDepartmentId(Long departmentId);

    List<DepartmentAgent> findByEmployeeId(Long employeeId);

    java.util.Optional<DepartmentAgent> findByDepartmentIdAndEmployeeId(Long departmentId, Long employeeId);

    boolean existsByEmployeeIdAndDepartmentId(Long employeeId, Long departmentId);

    /*Find all agents belonging to a particular department whose employee is currently ACTIVE*/
    @Query("""
        SELECT da
        FROM DepartmentAgent da
        JOIN da.employee e
        WHERE da.department.id = :departmentId
          AND e.employmentStatus = 'ACTIVE'
        """)
    List<DepartmentAgent> findEligibleAgents(
            @Param("departmentId") Long departmentId
    );
}