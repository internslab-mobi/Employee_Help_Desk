package xyz.mobi.employeehelpdesk.service;

import xyz.mobi.employeehelpdesk.dto.skill.CreateSkillRequest;
import xyz.mobi.employeehelpdesk.dto.skill.SkillResponse;
import xyz.mobi.employeehelpdesk.dto.skill.UpdateSkillRequest;

import java.util.List;

public interface SkillAdminService {

    SkillResponse createSkill(CreateSkillRequest request);

    List<SkillResponse> getAllSkills(Boolean activeOnly);

    SkillResponse getSkillById(Long id);

    SkillResponse updateSkill(Long id, UpdateSkillRequest request);

    void deleteSkill(Long id);
}
