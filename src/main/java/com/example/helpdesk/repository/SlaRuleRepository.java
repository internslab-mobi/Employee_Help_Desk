package com.example.helpdesk.repository;

import com.example.helpdesk.entity.SlaRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SlaRuleRepository extends JpaRepository<SlaRule, Long> {

    SlaRule findByDepartmentIdAndSubCategoryIdAndActiveTrue(Long departmentId, Long subCategoryId);
}
