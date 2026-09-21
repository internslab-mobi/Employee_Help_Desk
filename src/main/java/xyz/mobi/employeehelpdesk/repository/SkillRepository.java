package xyz.mobi.employeehelpdesk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.mobi.employeehelpdesk.entity.Skill;

import java.util.List;
import java.util.Optional;

public interface SkillRepository
        extends JpaRepository<Skill, Long> {

    Optional<Skill> findByName(String name);

    List<Skill> findByIsActiveTrue();

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}