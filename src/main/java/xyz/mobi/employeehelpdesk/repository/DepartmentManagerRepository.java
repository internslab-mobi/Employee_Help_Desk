package xyz.mobi.employeehelpdesk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import xyz.mobi.employeehelpdesk.entity.DepartmentManager;

import java.util.List;
import java.util.Optional;

public interface DepartmentManagerRepository
        extends JpaRepository<DepartmentManager, Long> {

    @Query("""
        SELECT dm
        FROM DepartmentManager dm
        JOIN FETCH dm.employee e
        LEFT JOIN FETCH e.department
        WHERE e.department.id = :departmentId
    """)
    List<DepartmentManager> findByDepartmentId(@Param("departmentId") Long departmentId);

    @Query("""
        SELECT dm
        FROM DepartmentManager dm
        JOIN FETCH dm.employee e
        LEFT JOIN FETCH e.department
        WHERE e.department.id = :departmentId
          AND dm.isPrimary = true
    """)
    Optional<DepartmentManager> findByDepartmentIdAndIsPrimaryTrue(
            @Param("departmentId") Long departmentId
    );

    @Query("""
        SELECT COUNT(dm) > 0
        FROM DepartmentManager dm
        WHERE dm.employee.id = :employeeId
          AND dm.employee.department.id = :departmentId
    """)
    boolean existsByEmployeeIdAndDepartmentId(
            @Param("employeeId") Long employeeId,
            @Param("departmentId") Long departmentId
    );
}