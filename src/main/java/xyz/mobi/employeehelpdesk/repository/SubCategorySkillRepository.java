package xyz.mobi.employeehelpdesk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import xyz.mobi.employeehelpdesk.entity.SubCategorySkill;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubCategorySkillRepository
        extends JpaRepository<SubCategorySkill, Long> {

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"skill"})
    List<SubCategorySkill> findBySubCategoryId(Long subCategoryId);

    List<SubCategorySkill> findBySkillId(Long skillId);

    boolean existsBySubCategoryIdAndSkillId(Long subCategoryId, Long skillId);

    Optional<SubCategorySkill> findBySubCategoryIdAndSkillId(Long subCategoryId, Long skillId);

    @Modifying
    @Transactional
    @Query("DELETE FROM SubCategorySkill scs WHERE scs.subCategory.id = :subCategoryId AND scs.skill.id = :skillId")
    void deleteBySubCategoryIdAndSkillId(@Param("subCategoryId") Long subCategoryId, @Param("skillId") Long skillId);

    @Modifying
    @Transactional
    @Query("DELETE FROM SubCategorySkill scs WHERE scs.subCategory.id = :subCategoryId")
    void deleteBySubCategoryId(@Param("subCategoryId") Long subCategoryId);

    @Modifying
    @Transactional
    @Query("DELETE FROM SubCategorySkill scs WHERE scs.skill.id = :skillId")
    void deleteBySkillId(@Param("skillId") Long skillId);
}