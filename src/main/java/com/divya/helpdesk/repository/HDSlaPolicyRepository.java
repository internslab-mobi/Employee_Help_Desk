package com.divya.helpdesk.repository;

import com.divya.helpdesk.entity.HDSlaPolicyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HDSlaPolicyRepository extends JpaRepository<HDSlaPolicyEntity, Long> {

    Optional<HDSlaPolicyEntity> findByDepartmentIdAndSubCategoryIdAndActiveTrue(Long departmentId, Long subCategoryId);
}