package com.divya.helpdesk.repository;

import com.divya.helpdesk.entity.HDEmployeeEntity;
import com.divya.helpdesk.enums.EmployeeRole;
import com.divya.helpdesk.enums.EmploymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

@Repository
public interface HDEmployeeRepository extends JpaRepository<HDEmployeeEntity, Long> {

    Optional<HDEmployeeEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmployeeCode(String employeeCode);

    List<HDEmployeeEntity> findByDepartmentIdAndRoleAndEmploymentStatusAndEnabledTrue(
            Long departmentId,
            EmployeeRole role,
            EmploymentStatus employmentStatus
    );

    List<HDEmployeeEntity> findByRoleAndEmploymentStatusAndEnabledTrue(
            EmployeeRole role,
            EmploymentStatus employmentStatus
    );

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM HDEmployeeEntity e ORDER BY e.id DESC")
    List<HDEmployeeEntity> findLastEmployeeForUpdate(Pageable pageable);
}