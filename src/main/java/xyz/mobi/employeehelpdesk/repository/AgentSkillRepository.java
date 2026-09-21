package xyz.mobi.employeehelpdesk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.mobi.employeehelpdesk.entity.AgentSkill;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface AgentSkillRepository
        extends JpaRepository<AgentSkill, Long> {

    List<AgentSkill> findByAgentId(Long agentId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"skill", "agent"})
    List<AgentSkill> findByAgentIdIn(java.util.Collection<Long> agentIds);

    List<AgentSkill> findBySkillId(Long skillId);

    boolean existsByAgentIdAndSkillId(
            Long agentId,
            Long skillId
    );

    java.util.Optional<AgentSkill> findByAgentIdAndSkillId(
            Long agentId,
            Long skillId
    );

    @Modifying
    @Transactional
    @Query("DELETE FROM AgentSkill a WHERE a.agent.id = :agentId")
    void deleteByAgentId(@Param("agentId") Long agentId);

    @Modifying
    @Transactional
    @Query("DELETE FROM AgentSkill a WHERE a.agent.id = :agentId AND a.skill.id = :skillId")
    void deleteByAgentIdAndSkillId(@Param("agentId") Long agentId, @Param("skillId") Long skillId);
}