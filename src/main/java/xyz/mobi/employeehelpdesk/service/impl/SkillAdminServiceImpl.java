package xyz.mobi.employeehelpdesk.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.mobi.employeehelpdesk.dto.skill.CreateSkillRequest;
import xyz.mobi.employeehelpdesk.dto.skill.SkillResponse;
import xyz.mobi.employeehelpdesk.dto.skill.UpdateSkillRequest;
import xyz.mobi.employeehelpdesk.entity.Skill;
import xyz.mobi.employeehelpdesk.entity.enums.UserRole;
import xyz.mobi.employeehelpdesk.exception.BadRequestException;
import xyz.mobi.employeehelpdesk.exception.ResourceNotFoundException;
import xyz.mobi.employeehelpdesk.repository.AgentSkillRepository;
import xyz.mobi.employeehelpdesk.repository.SkillRepository;
import xyz.mobi.employeehelpdesk.repository.SubCategorySkillRepository;
import xyz.mobi.employeehelpdesk.service.CurrentUserService;
import xyz.mobi.employeehelpdesk.service.SkillAdminService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkillAdminServiceImpl implements SkillAdminService {

    private final SkillRepository skillRepository;
    private final AgentSkillRepository agentSkillRepository;
    private final SubCategorySkillRepository subCategorySkillRepository;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public SkillResponse createSkill(CreateSkillRequest request) {
        checkAdminRole();

        if (skillRepository.existsByName(request.name().trim())) {
            throw new BadRequestException("Skill name already exists: " + request.name());
        }

        Skill skill = new Skill();
        skill.setName(request.name().trim());
        skill.setDescription(request.description() != null ? request.description().trim() : null);
        skill.setIsActive(true);

        Skill saved = skillRepository.save(skill);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SkillResponse> getAllSkills(Boolean activeOnly) {
        List<Skill> list = Boolean.TRUE.equals(activeOnly)
                ? skillRepository.findByIsActiveTrue()
                : skillRepository.findAll();

        return list.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SkillResponse getSkillById(Long id) {
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found with id: " + id));
        return toResponse(skill);
    }

    @Override
    @Transactional
    public SkillResponse updateSkill(Long id, UpdateSkillRequest request) {
        checkAdminRole();

        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found with id: " + id));

        if (skillRepository.existsByNameAndIdNot(request.name().trim(), id)) {
            throw new BadRequestException("Skill name already exists: " + request.name());
        }

        skill.setName(request.name().trim());
        skill.setDescription(request.description() != null ? request.description().trim() : null);
        skill.setIsActive(request.isActive());

        Skill updated = skillRepository.save(skill);
        return toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteSkill(Long id) {
        checkAdminRole();

        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found with id: " + id));

        if (!agentSkillRepository.findBySkillId(id).isEmpty()) {
            throw new BadRequestException("Cannot delete skill assigned to agents. Deactivate the skill instead.");
        }

        if (!subCategorySkillRepository.findBySkillId(id).isEmpty()) {
            throw new BadRequestException("Cannot delete skill required by subcategories. Deactivate the skill instead.");
        }

        try {
            skillRepository.delete(skill);
        } catch (Exception ex) {
            throw new BadRequestException("Cannot delete skill due to existing references. Deactivate the skill instead.");
        }
    }

    private void checkAdminRole() {
        UserRole role = currentUserService.getCurrentUserRole();
        if (role != UserRole.ADMIN) {
            throw new AccessDeniedException("Access denied: ADMIN role required to manage skills");
        }
    }

    private SkillResponse toResponse(Skill skill) {
        return SkillResponse.builder()
                .id(skill.getId())
                .name(skill.getName())
                .description(skill.getDescription())
                .isActive(skill.getIsActive())
                .createdAt(skill.getCreatedAt())
                .updatedAt(skill.getUpdatedAt())
                .build();
    }
}
