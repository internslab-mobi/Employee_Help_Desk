package com.divya.helpdesk.repository;

import com.divya.helpdesk.entity.HDEmployee;
import com.divya.helpdesk.enums.HDEmploymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HDEmployeeRepository extends JpaRepository<HDEmployee, Long> {
    Optional<HDEmployee> findByEmail(String email);
    boolean existsByEmployeeCode(String employeeCode);
    boolean existsByEmail(String email);
    List<HDEmployee> findByDepartmentId(Long departmentId);
}
