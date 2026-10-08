package xyz.mobi.employeehelpdesk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import xyz.mobi.employeehelpdesk.entity.SubCategorySkill;

import java.util.List;

@Repository
public interface SubCategorySkillRepository
        extends JpaRepository<SubCategorySkill, Long> {

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"skill"})
    List<SubCategorySkill> findBySubCategoryId(Long subCategoryId);

}