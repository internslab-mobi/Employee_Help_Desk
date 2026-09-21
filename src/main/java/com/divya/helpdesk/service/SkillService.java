package com.divya.helpdesk.service;

import com.divya.helpdesk.dto.request.AgentSkillAssignRequest;
import com.divya.helpdesk.dto.request.SkillCreateRequest;
import com.divya.helpdesk.dto.request.SkillUpdateRequest;
import com.divya.helpdesk.dto.request.SubCategorySkillAssignRequest;
import com.divya.helpdesk.dto.response.AgentSkillResponse;
import com.divya.helpdesk.dto.response.SkillResponse;
import com.divya.helpdesk.dto.response.SubCategorySkillResponse;

import java.util.List;

public interface SkillService {
    SkillResponse createSkill(SkillCreateRequest request);
    SkillResponse updateSkill(Long id, SkillUpdateRequest request);
    void deleteSkill(Long id);
    SkillResponse getSkillById(Long id);
    List<SkillResponse> getAllSkills();

    // Agent Skill operations
    AgentSkillResponse assignSkillToAgent(AgentSkillAssignRequest request);
    void removeSkillFromAgent(Long agentId, Long skillId);
    List<AgentSkillResponse> getSkillsByAgent(Long agentId);

    // SubCategory Skill operations
    SubCategorySkillResponse assignSkillToSubCategory(SubCategorySkillAssignRequest request);
    void removeSkillFromSubCategory(Long subCategoryId, Long skillId);
    List<SubCategorySkillResponse> getSkillsBySubCategory(Long subCategoryId);
    List<SubCategorySkillResponse> getSubCategoriesBySkill(Long skillId);
}
