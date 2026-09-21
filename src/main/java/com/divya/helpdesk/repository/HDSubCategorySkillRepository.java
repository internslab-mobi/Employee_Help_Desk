package com.divya.helpdesk.repository;

import com.divya.helpdesk.entity.HDSubCategorySkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HDSubCategorySkillRepository extends JpaRepository<HDSubCategorySkill, Long> {
    List<HDSubCategorySkill> findBySubCategoryId(Long subCategoryId);
    Optional<HDSubCategorySkill> findBySubCategoryIdAndSkillId(Long subCategoryId, Long skillId);
    boolean existsBySubCategoryIdAndSkillId(Long subCategoryId, Long skillId);
    List<HDSubCategorySkill> findBySkillId(Long skillId);
}
