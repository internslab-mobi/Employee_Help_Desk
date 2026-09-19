package com.example.helpdesk.repository;

import com.example.helpdesk.entity.DepartmentAgent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentAgentRepository extends JpaRepository<DepartmentAgent, Long> {
    List<DepartmentAgent> findByDepartmentId(Long departmentId);
    Optional<DepartmentAgent> findByEmployeeId(Long employeeId);
}
