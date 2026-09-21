package xyz.mobi.employeehelpdesk.service.helperservice;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import xyz.mobi.employeehelpdesk.entity.DepartmentAgent;
import xyz.mobi.employeehelpdesk.entity.AgentSkill;
import xyz.mobi.employeehelpdesk.entity.SubCategorySkill;
import xyz.mobi.employeehelpdesk.repository.AgentSkillRepository;
import xyz.mobi.employeehelpdesk.repository.SubCategorySkillRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SkillMatchingService {

    private final AgentSkillRepository agentSkillRepository;
    private final SubCategorySkillRepository subCategorySkillRepository;

    public SkillMatchResult calculateMatch(
            DepartmentAgent agent,
            Long subCategoryId) {

        List<SubCategorySkill> requiredSkills =
                subCategorySkillRepository
                        .findBySubCategoryId(subCategoryId);

        List<AgentSkill> agentSkills =
                agentSkillRepository
                        .findByAgentId(agent.getId());

        Set<Long> requiredSkillIds = new HashSet<>();

        for (SubCategorySkill requiredSkill : requiredSkills) {
            requiredSkillIds.add(
                    requiredSkill.getSkill().getId()
            );
        }

        Set<Long> agentSkillIds = new HashSet<>();

        for (AgentSkill agentSkill : agentSkills) {
            agentSkillIds.add(
                    agentSkill.getSkill().getId()
            );
        }

        int matchedSkillCount = 0;

        for (Long requiredSkillId : requiredSkillIds) {
            if (agentSkillIds.contains(requiredSkillId)) {
                matchedSkillCount++;
            }
        }

        return new SkillMatchResult(
                matchedSkillCount,
                requiredSkillIds.size()
        );
    }

}