package com.divya.helpdesk.repository;

import com.divya.helpdesk.entity.HDEmployee;
import com.divya.helpdesk.enums.EmployeeRole;
import com.divya.helpdesk.enums.EmploymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HDEmployeeRepository extends JpaRepository<HDEmployee, Long> {

    Optional<HDEmployee> findByEmail(String email);

    Optional<HDEmployee> findByEmployeeCodeAndEmail(
            String employeeCode,
            String email
    );

    boolean existsByEmail(String email);

    boolean existsByEmployeeCode(String employeeCode);

    List<HDEmployee> findByDepartmentIdAndRoleAndEnabledTrue(
            Long departmentId,
            EmployeeRole role
    );

    List<HDEmployee> findByDepartment_IdAndRoleAndEmploymentStatusAndEnabledTrue(
            Long departmentId,
            EmployeeRole role,
            EmploymentStatus employmentStatus
    );
    List<HDEmployee> findByRoleAndEmploymentStatusAndEnabledTrue(
            EmployeeRole role,
            EmploymentStatus employmentStatus
    );

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT e FROM HDEmployee e ORDER BY e.id DESC")
    List<HDEmployee> findLastEmployeeForUpdate(org.springframework.data.domain.Pageable pageable);
}