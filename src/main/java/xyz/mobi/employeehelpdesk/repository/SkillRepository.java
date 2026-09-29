package xyz.mobi.employeehelpdesk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.mobi.employeehelpdesk.entity.Skill;

public interface SkillRepository
        extends JpaRepository<Skill, Long> {

}