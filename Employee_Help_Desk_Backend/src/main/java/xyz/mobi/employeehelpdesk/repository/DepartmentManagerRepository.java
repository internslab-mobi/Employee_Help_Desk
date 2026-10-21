package xyz.mobi.employeehelpdesk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import xyz.mobi.employeehelpdesk.entity.DepartmentManager;

import java.util.List;
import java.util.Optional;

public interface DepartmentManagerRepository
        extends JpaRepository<DepartmentManager, Long> {


    Optional<DepartmentManager> findByEmployeeId(Long employeeId);
}