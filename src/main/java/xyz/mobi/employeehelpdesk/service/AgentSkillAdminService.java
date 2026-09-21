package xyz.mobi.employeehelpdesk.service;

import xyz.mobi.employeehelpdesk.dto.agentskill.AgentSkillRequest;
import xyz.mobi.employeehelpdesk.dto.agentskill.AgentSkillResponse;

import java.util.List;

public interface AgentSkillAdminService {

    AgentSkillResponse assignSkillToAgent(Long agentId, AgentSkillRequest request);

    List<AgentSkillResponse> getAgentSkills(Long agentId);

    void removeSkillFromAgent(Long agentId, Long skillId);
}
