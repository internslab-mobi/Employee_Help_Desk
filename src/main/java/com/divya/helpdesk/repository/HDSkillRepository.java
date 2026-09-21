package com.divya.helpdesk.repository;

import com.divya.helpdesk.entity.HDSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HDSkillRepository extends JpaRepository<HDSkill, Long> {
    boolean existsByName(String name);
}
