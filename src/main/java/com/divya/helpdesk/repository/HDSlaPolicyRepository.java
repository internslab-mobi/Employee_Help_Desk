package com.divya.helpdesk.repository;

import com.divya.helpdesk.entity.HDSlaPolicy;
import com.divya.helpdesk.enums.TicketPriority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HDSlaPolicyRepository extends JpaRepository<HDSlaPolicy, Long> {

    Optional<HDSlaPolicy> findByDepartment_IdAndSubCategory_IdAndActiveTrue(Long departmentId, Long subCategoryId);

}