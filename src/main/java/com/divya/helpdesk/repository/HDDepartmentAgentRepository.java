package com.divya.helpdesk.repository;

import com.divya.helpdesk.entity.HDDepartmentAgent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HDDepartmentAgentRepository extends JpaRepository<HDDepartmentAgent, Long> {
    List<HDDepartmentAgent> findByDepartmentId(Long departmentId);
    Optional<HDDepartmentAgent> findByDepartmentIdAndEmployeeId(Long departmentId, Long employeeId);
    boolean existsByDepartmentIdAndEmployeeId(Long departmentId, Long employeeId);

    @Query("SELECT da FROM HDDepartmentAgent da WHERE da.department.id = :departmentId AND da.employee.employmentStatus = 'ACTIVE'")
    List<HDDepartmentAgent> findActiveAgentsByDepartmentId(@Param("departmentId") Long departmentId);
}
