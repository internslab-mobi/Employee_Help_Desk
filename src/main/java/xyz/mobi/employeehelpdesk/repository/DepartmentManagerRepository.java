package xyz.mobi.employeehelpdesk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.mobi.employeehelpdesk.entity.DepartmentManager;

import java.util.List;

public interface DepartmentManagerRepository
        extends JpaRepository<DepartmentManager, Long> {

    List<DepartmentManager> findByDepartmentId(Long departmentId);

    List<DepartmentManager> findByEmployeeId(Long employeeId);

    java.util.Optional<DepartmentManager> findByDepartmentIdAndEmployeeId(Long departmentId, Long employeeId);

    java.util.Optional<DepartmentManager> findByDepartmentIdAndIsPrimaryTrue(Long departmentId);

    boolean existsByEmployeeIdAndDepartmentId(Long employeeId, Long departmentId);
}