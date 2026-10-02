package com.divya.helpdesk.repository;

import com.divya.helpdesk.entity.HDSubCategorySkillEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HDSubCategorySkillRepository extends JpaRepository<HDSubCategorySkillEntity, Long> {

    List<HDSubCategorySkillEntity> findBySubCategoryId(Long subCategoryId);
}