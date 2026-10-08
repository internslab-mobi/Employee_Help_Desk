package xyz.mobi.employeehelpdesk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.mobi.employeehelpdesk.entity.Department;

public interface DepartmentRepository
        extends JpaRepository<Department, Long> {
}