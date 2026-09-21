package com.divya.helpdesk.repository;

import com.divya.helpdesk.entity.HDAgentSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HDAgentSkillRepository extends JpaRepository<HDAgentSkill, Long> {
    List<HDAgentSkill> findByAgentId(Long agentId);
    Optional<HDAgentSkill> findByAgentIdAndSkillId(Long agentId, Long skillId);
    boolean existsByAgentIdAndSkillId(Long agentId, Long skillId);
}
