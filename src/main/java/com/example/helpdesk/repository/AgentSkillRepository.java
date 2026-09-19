package com.example.helpdesk.repository;

import com.example.helpdesk.entity.AgentSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentSkillRepository
        extends JpaRepository<AgentSkill, Long> {

    List<AgentSkill> findByAgentId(Long agentId);
}