package xyz.mobi.employeehelpdesk.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import xyz.mobi.employeehelpdesk.entity.Employee;
import xyz.mobi.employeehelpdesk.entity.enums.EmploymentStatus;
import xyz.mobi.employeehelpdesk.entity.enums.UserRole;

import java.util.Optional;

public interface EmployeeRepository
        extends JpaRepository<Employee, Long> {

    @EntityGraph(attributePaths = {"department"})
    Optional<Employee> findById(Long id);

    @EntityGraph(attributePaths = {"department"})
    Optional<Employee> findByEmail(String email);

    Boolean existsByEmail(String email);

    @EntityGraph(attributePaths = {"department"})
    @Query(value = """
        SELECT e
        FROM Employee e
        WHERE (:departmentId IS NULL OR e.department.id = :departmentId)
          AND (:status IS NULL OR e.employmentStatus = :status)
          AND (:role IS NULL OR e.role = :role)
          AND (
                :search IS NULL
                OR LOWER(e.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(e.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(CONCAT(e.firstName, ' ', e.lastName))
                    LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(e.employeeCode) LIKE LOWER(CONCAT('%', :search, '%'))
              )
    """, countQuery = """
        SELECT COUNT(e)
        FROM Employee e
        WHERE (:departmentId IS NULL OR e.department.id = :departmentId)
          AND (:status IS NULL OR e.employmentStatus = :status)
          AND (:role IS NULL OR e.role = :role)
          AND (
                :search IS NULL
                OR LOWER(e.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(e.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(CONCAT(e.firstName, ' ', e.lastName))
                    LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(e.employeeCode) LIKE LOWER(CONCAT('%', :search, '%'))
              )
    """)
    Page<Employee> searchEmployees(
            @Param("departmentId") Long departmentId,
            @Param("status") EmploymentStatus status,
            @Param("role") UserRole role,
            @Param("search") String search,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"department"})
    @Query(value = """
        SELECT e
        FROM Employee e
        WHERE (:departmentId IS NULL OR e.department.id = :departmentId)
    """, countQuery = """
        SELECT COUNT(e)
        FROM Employee e
        WHERE (:departmentId IS NULL OR e.department.id = :departmentId)
    """)
    Page<Employee> findAllEmployees(
            @Param("departmentId") Long departmentId,
            Pageable pageable
    );
}