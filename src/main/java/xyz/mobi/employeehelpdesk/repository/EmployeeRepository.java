package xyz.mobi.employeehelpdesk.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import xyz.mobi.employeehelpdesk.entity.Employee;
import xyz.mobi.employeehelpdesk.entity.enums.EmploymentStatus;
import xyz.mobi.employeehelpdesk.entity.enums.UserRole;

import java.util.Optional;

public interface EmployeeRepository
        extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmployeeCode(String employeeCode);

    Optional<Employee> findByEmail(String email);

    boolean existsByEmployeeCode(String employeeCode);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByEmployeeCodeAndIdNot(String employeeCode, Long id);

    Page<Employee> findByDepartmentId(Long departmentId, Pageable pageable);

    Page<Employee> findByEmploymentStatus(EmploymentStatus employmentStatus, Pageable pageable);

    Page<Employee> findByRole(UserRole role, Pageable pageable);
}