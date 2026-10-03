package com.example.helpdesk.repository;

import com.example.helpdesk.entity.DepartmentManager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentManagerRepository extends JpaRepository<DepartmentManager, Long> {

    List<DepartmentManager> findByDepartmentId(Long departmentId);

    Optional<DepartmentManager> findByDepartmentIdAndPrimaryTrue(Long departmentId);

    boolean existsByDepartmentIdAndEmployeeId(Long departmentId, Long employeeId);
}



