package xyz.mobi.employeehelpdesk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.mobi.employeehelpdesk.entity.Department;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository
        extends JpaRepository<Department, Long> {

    List<Department> findByIsActiveTrueOrderByNameAsc();

    Optional<Department> findByIdAndIsActiveTrue(Long id);
}