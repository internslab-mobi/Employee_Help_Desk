package com.divya.helpdesk.repository;

import com.divya.helpdesk.entity.HDSubCategorySkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HDSubCategorySkillRepository extends JpaRepository<HDSubCategorySkill, Long> {

    List<HDSubCategorySkill> findBySubCategory_Id(Long subCategoryId);
}