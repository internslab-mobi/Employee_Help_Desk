package xyz.mobi.employeehelpdesk.service;

import xyz.mobi.employeehelpdesk.dto.skill.AssignSkillsRequestDTO;
import xyz.mobi.employeehelpdesk.dto.skill.CreateSkillRequestDTO;
import xyz.mobi.employeehelpdesk.dto.skill.SkillResponseDTO;

import java.util.List;

public interface SkillService {
    List<SkillResponseDTO> getAllSkills();
    SkillResponseDTO createSkill(CreateSkillRequestDTO request);
    List<SkillResponseDTO> getAgentSkills(Long agentOrEmployeeId);
    List<SkillResponseDTO> assignAgentSkills(Long agentOrEmployeeId, AssignSkillsRequestDTO request);
    List<SkillResponseDTO> getSubCategorySkills(Long subCategoryId);
    List<SkillResponseDTO> assignSubCategorySkills(Long subCategoryId, AssignSkillsRequestDTO request);
}
