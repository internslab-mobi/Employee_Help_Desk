package com.divya.helpdesk.repository;

import com.divya.helpdesk.entity.HDDepartmentManager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HDDepartmentManagerRepository extends JpaRepository<HDDepartmentManager, Long> {
    List<HDDepartmentManager> findByDepartmentId(Long departmentId);
    boolean existsByDepartmentIdAndEmployeeId(Long departmentId, Long employeeId);

    @Query("SELECT dm FROM HDDepartmentManager dm WHERE dm.department.id = :departmentId ORDER BY dm.isPrimary DESC, dm.id ASC")
    List<HDDepartmentManager> findManagersByDepartmentIdOrderedByPrimary(@Param("departmentId") Long departmentId);
}
