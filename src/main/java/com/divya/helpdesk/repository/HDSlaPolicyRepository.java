package com.divya.helpdesk.repository;

import com.divya.helpdesk.entity.HDSlaPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HDSlaPolicyRepository extends JpaRepository<HDSlaPolicy, Long> {
    List<HDSlaPolicy> findByDepartmentId(Long departmentId);
    Optional<HDSlaPolicy> findByDepartmentIdAndSubCategoryId(Long departmentId, Long subCategoryId);
    boolean existsByDepartmentIdAndSubCategoryId(Long departmentId, Long subCategoryId);
}
