package com.divya.helpdesk.repository;

import com.divya.helpdesk.entity.HDCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HDCategoryRepository extends JpaRepository<HDCategoryEntity, Long> {
}
