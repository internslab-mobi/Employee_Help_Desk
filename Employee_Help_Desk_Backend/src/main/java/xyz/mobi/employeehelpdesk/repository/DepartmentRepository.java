package xyz.mobi.employeehelpdesk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.mobi.employeehelpdesk.entity.Department;

import java.util.List;

public interface DepartmentRepository
        extends JpaRepository<Department, Long> {
    List<Department> findByIsActiveTrue();
}