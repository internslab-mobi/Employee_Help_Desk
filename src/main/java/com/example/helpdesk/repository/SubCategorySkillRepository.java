package com.example.helpdesk.repository;

import com.example.helpdesk.entity.SubCategorySkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubCategorySkillRepository
        extends JpaRepository<SubCategorySkill, Long> {

    List<SubCategorySkill> findBySubCategoryId(Long subCategoryId);
}