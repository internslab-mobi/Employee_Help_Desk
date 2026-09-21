package com.divya.helpdesk.repository;

import com.divya.helpdesk.entity.HDCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HDCategoryRepository extends JpaRepository<HDCategory, Long> {
    List<HDCategory> findByDepartmentId(Long departmentId);
    boolean existsByDepartmentIdAndName(Long departmentId, String name);
}
