package xyz.mobi.employeehelpdesk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.mobi.employeehelpdesk.entity.AgentSkill;

import java.util.List;

public interface AgentSkillRepository
        extends JpaRepository<AgentSkill, Long> {

    List<AgentSkill> findByAgentId(Long agentId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"skill", "agent"})
    List<AgentSkill> findByAgentIdIn(java.util.Collection<Long> agentIds);

}