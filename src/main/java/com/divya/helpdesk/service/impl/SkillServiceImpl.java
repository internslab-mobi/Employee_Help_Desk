package com.divya.helpdesk.service.impl;

import com.divya.helpdesk.dto.request.AgentSkillAssignRequest;
import com.divya.helpdesk.dto.request.SkillCreateRequest;
import com.divya.helpdesk.dto.request.SkillUpdateRequest;
import com.divya.helpdesk.dto.request.SubCategorySkillAssignRequest;
import com.divya.helpdesk.dto.response.AgentSkillResponse;
import com.divya.helpdesk.dto.response.SkillResponse;
import com.divya.helpdesk.dto.response.SubCategorySkillResponse;
import com.divya.helpdesk.entity.HDAgentSkill;
import com.divya.helpdesk.entity.HDDepartmentAgent;
import com.divya.helpdesk.entity.HDSkill;
import com.divya.helpdesk.entity.HDSubCategory;
import com.divya.helpdesk.entity.HDSubCategorySkill;
import com.divya.helpdesk.exception.BadRequestException;
import com.divya.helpdesk.exception.ResourceNotFoundException;
import com.divya.helpdesk.mapper.SkillMapper;
import com.divya.helpdesk.repository.HDAgentSkillRepository;
import com.divya.helpdesk.repository.HDDepartmentAgentRepository;
import com.divya.helpdesk.repository.HDSkillRepository;
import com.divya.helpdesk.repository.HDSubCategoryRepository;
import com.divya.helpdesk.repository.HDSubCategorySkillRepository;
import com.divya.helpdesk.service.SkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SkillServiceImpl implements SkillService {

    private final HDSkillRepository skillRepository;
    private final HDDepartmentAgentRepository departmentAgentRepository;
    private final HDAgentSkillRepository agentSkillRepository;
    private final HDSubCategoryRepository subCategoryRepository;
    private final HDSubCategorySkillRepository subCategorySkillRepository;
    private final SkillMapper skillMapper;

    @Override
    public SkillResponse createSkill(SkillCreateRequest request) {
        if (skillRepository.existsByName(request.getName())) {
            throw new BadRequestException("Skill with name '" + request.getName() + "' already exists");
        }
        HDSkill skill = skillMapper.toEntity(request);
        HDSkill saved = skillRepository.save(skill);
        return skillMapper.toResponse(saved);
    }

    @Override
    public SkillResponse updateSkill(Long id, SkillUpdateRequest request) {
        HDSkill skill = skillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found with id: " + id));

        if (!skill.getName().equalsIgnoreCase(request.getName()) && skillRepository.existsByName(request.getName())) {
            throw new BadRequestException("Skill with name '" + request.getName() + "' already exists");
        }

        skillMapper.updateEntity(skill, request);
        HDSkill saved = skillRepository.save(skill);
        return skillMapper.toResponse(saved);
    }

    @Override
    public void deleteSkill(Long id) {
        if (!skillRepository.existsById(id)) {
            throw new ResourceNotFoundException("Skill not found with id: " + id);
        }
        skillRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public SkillResponse getSkillById(Long id) {
        HDSkill skill = skillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found with id: " + id));
        return skillMapper.toResponse(skill);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SkillResponse> getAllSkills() {
        return skillRepository.findAll().stream()
                .map(skillMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public AgentSkillResponse assignSkillToAgent(AgentSkillAssignRequest request) {
        HDDepartmentAgent agent = departmentAgentRepository.findById(request.getAgentId())
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found with id: " + request.getAgentId()));

        HDSkill skill = skillRepository.findById(request.getSkillId())
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found with id: " + request.getSkillId()));

        if (agentSkillRepository.existsByAgentIdAndSkillId(request.getAgentId(), request.getSkillId())) {
            throw new BadRequestException("Skill is already assigned to this agent");
        }

        HDAgentSkill agentSkill = new HDAgentSkill();
        agentSkill.setAgent(agent);
        agentSkill.setSkill(skill);

        HDAgentSkill saved = agentSkillRepository.save(agentSkill);
        return skillMapper.toAgentSkillResponse(saved);
    }

    @Override
    public void removeSkillFromAgent(Long agentId, Long skillId) {
        HDAgentSkill agentSkill = agentSkillRepository.findByAgentIdAndSkillId(agentId, skillId)
                .orElseThrow(() -> new ResourceNotFoundException("Skill assignment not found for agent id " + agentId + " and skill id " + skillId));
        agentSkillRepository.delete(agentSkill);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgentSkillResponse> getSkillsByAgent(Long agentId) {
        if (!departmentAgentRepository.existsById(agentId)) {
            throw new ResourceNotFoundException("Agent not found with id: " + agentId);
        }
        return agentSkillRepository.findByAgentId(agentId).stream()
                .map(skillMapper::toAgentSkillResponse)
                .collect(Collectors.toList());
    }

    @Override
    public SubCategorySkillResponse assignSkillToSubCategory(SubCategorySkillAssignRequest request) {
        HDSubCategory subCategory = subCategoryRepository.findById(request.getSubCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("SubCategory not found with id: " + request.getSubCategoryId()));

        HDSkill skill = skillRepository.findById(request.getSkillId())
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found with id: " + request.getSkillId()));

        if (subCategorySkillRepository.existsBySubCategoryIdAndSkillId(request.getSubCategoryId(), request.getSkillId())) {
            throw new BadRequestException("Skill is already assigned to this subcategory");
        }

        HDSubCategorySkill subCategorySkill = new HDSubCategorySkill();
        subCategorySkill.setSubCategory(subCategory);
        subCategorySkill.setSkill(skill);

        HDSubCategorySkill saved = subCategorySkillRepository.save(subCategorySkill);
        return skillMapper.toSubCategorySkillResponse(saved);
    }

    @Override
    public void removeSkillFromSubCategory(Long subCategoryId, Long skillId) {
        HDSubCategorySkill subCategorySkill = subCategorySkillRepository.findBySubCategoryIdAndSkillId(subCategoryId, skillId)
                .orElseThrow(() -> new ResourceNotFoundException("Skill assignment not found for subcategory id " + subCategoryId + " and skill id " + skillId));
        subCategorySkillRepository.delete(subCategorySkill);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubCategorySkillResponse> getSkillsBySubCategory(Long subCategoryId) {
        if (!subCategoryRepository.existsById(subCategoryId)) {
            throw new ResourceNotFoundException("SubCategory not found with id: " + subCategoryId);
        }
        return subCategorySkillRepository.findBySubCategoryId(subCategoryId).stream()
                .map(skillMapper::toSubCategorySkillResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubCategorySkillResponse> getSubCategoriesBySkill(Long skillId) {
        if (!skillRepository.existsById(skillId)) {
            throw new ResourceNotFoundException("Skill not found with id: " + skillId);
        }
        return subCategorySkillRepository.findBySkillId(skillId).stream()
                .map(skillMapper::toSubCategorySkillResponse)
                .collect(Collectors.toList());
    }
}
