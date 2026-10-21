package xyz.mobi.employeehelpdesk.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.mobi.employeehelpdesk.dto.skill.AssignSkillsRequestDTO;
import xyz.mobi.employeehelpdesk.dto.skill.CreateSkillRequestDTO;
import xyz.mobi.employeehelpdesk.dto.skill.SkillResponseDTO;
import xyz.mobi.employeehelpdesk.entity.AgentSkill;
import xyz.mobi.employeehelpdesk.entity.DepartmentAgent;
import xyz.mobi.employeehelpdesk.entity.Skill;
import xyz.mobi.employeehelpdesk.entity.SubCategory;
import xyz.mobi.employeehelpdesk.entity.SubCategorySkill;
import xyz.mobi.employeehelpdesk.exception.BadRequestException;
import xyz.mobi.employeehelpdesk.exception.ResourceNotFoundException;
import xyz.mobi.employeehelpdesk.repository.AgentSkillRepository;
import xyz.mobi.employeehelpdesk.repository.DepartmentAgentRepository;
import xyz.mobi.employeehelpdesk.repository.SkillRepository;
import xyz.mobi.employeehelpdesk.repository.SubCategoryRepository;
import xyz.mobi.employeehelpdesk.repository.SubCategorySkillRepository;
import xyz.mobi.employeehelpdesk.service.SkillService;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SkillServiceImpl implements SkillService {

    private final SkillRepository skillRepository;
    private final AgentSkillRepository agentSkillRepository;
    private final SubCategorySkillRepository subCategorySkillRepository;
    private final DepartmentAgentRepository departmentAgentRepository;
    private final SubCategoryRepository subCategoryRepository;

    @Override
    public List<SkillResponseDTO> getAllSkills() {
        log.debug("Fetching all skills");
        return skillRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public SkillResponseDTO createSkill(CreateSkillRequestDTO request) {
        log.info("Creating skill: name={}", request.name());
        String name = request.name().trim();
        boolean exists = skillRepository.findAll().stream()
                .anyMatch(s -> s.getName().equalsIgnoreCase(name));
        if (exists) {
            throw new BadRequestException("Skill with name '" + name + "' already exists");
        }

        Skill skill = new Skill();
        skill.setName(name);
        skill.setDescription(request.description() != null ? request.description().trim() : null);
        skill.setIsActive(true);

        Skill saved = skillRepository.save(skill);
        return toResponse(saved);
    }

    @Override
    public List<SkillResponseDTO> getAgentSkills(Long agentOrEmployeeId) {
        DepartmentAgent agent = resolveAgent(agentOrEmployeeId);
        List<AgentSkill> agentSkills = agentSkillRepository.findByAgentId(agent.getId());
        return agentSkills.stream()
                .map(as -> toResponse(as.getSkill()))
                .toList();
    }

    @Override
    @Transactional
    public List<SkillResponseDTO> assignAgentSkills(Long agentOrEmployeeId, AssignSkillsRequestDTO request) {
        DepartmentAgent agent = resolveAgent(agentOrEmployeeId);
        log.info("Assigning skills to agentId={}: {}", agent.getId(), request != null ? request.skillIds() : null);

        Set<Long> existingSkillIds = agentSkillRepository.findByAgentId(agent.getId()).stream()
                .map(as -> as.getSkill().getId())
                .collect(Collectors.toSet());

        if (request != null && request.skillIds() != null && !request.skillIds().isEmpty()) {
            for (Long skillId : request.skillIds()) {
                if(!existingSkillIds.contains(skillId)) {
                    skillRepository.findById(skillId).ifPresent(skill -> {
                        AgentSkill as = new AgentSkill();
                        as.setAgent(agent);
                        as.setSkill(skill);
                        agentSkillRepository.save(as);
                    });
                }
            }
        }

        return getAgentSkills(agent.getId());
    }

    @Override
    public List<SkillResponseDTO> getSubCategorySkills(Long subCategoryId) {
        List<SubCategorySkill> skills = subCategorySkillRepository.findBySubCategoryId(subCategoryId);
        return skills.stream()
                .map(scs -> toResponse(scs.getSkill()))
                .toList();
    }

    @Override
    @Transactional
    public List<SkillResponseDTO> assignSubCategorySkills(Long subCategoryId, AssignSkillsRequestDTO request) {
        SubCategory subCategory = subCategoryRepository.findById(subCategoryId)
                .orElseThrow(() -> new ResourceNotFoundException("SubCategory not found: " + subCategoryId));
        log.info("Assigning skills to subCategoryId={}: {}", subCategoryId, request != null ? request.skillIds() : null);

        subCategorySkillRepository.deleteBySubCategoryId(subCategory.getId());

        if (request != null && request.skillIds() != null && !request.skillIds().isEmpty()) {
            for (Long skillId : request.skillIds()) {
                skillRepository.findById(skillId).ifPresent(skill -> {
                    SubCategorySkill scs = new SubCategorySkill();
                    scs.setSubCategory(subCategory);
                    scs.setSkill(skill);
                    subCategorySkillRepository.save(scs);
                });
            }
        }

        return getSubCategorySkills(subCategory.getId());
    }

    private DepartmentAgent resolveAgent(Long id) {
        return departmentAgentRepository.findById(id)
                .or(() -> departmentAgentRepository.findByEmployeeId(id))
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found with id or employeeId: " + id));
    }

    private SkillResponseDTO toResponse(Skill skill) {
        if (skill == null) return null;
        return SkillResponseDTO.builder()
                .id(skill.getId())
                .name(skill.getName())
                .description(skill.getDescription())
                .isActive(skill.getIsActive())
                .build();
    }
}
