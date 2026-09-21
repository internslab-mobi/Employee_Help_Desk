package xyz.mobi.employeehelpdesk.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.mobi.employeehelpdesk.dto.subcategoryskill.SubCategorySkillRequest;
import xyz.mobi.employeehelpdesk.dto.subcategoryskill.SubCategorySkillResponse;
import xyz.mobi.employeehelpdesk.entity.Skill;
import xyz.mobi.employeehelpdesk.entity.SubCategory;
import xyz.mobi.employeehelpdesk.entity.SubCategorySkill;
import xyz.mobi.employeehelpdesk.entity.enums.UserRole;
import xyz.mobi.employeehelpdesk.exception.BadRequestException;
import xyz.mobi.employeehelpdesk.exception.ResourceNotFoundException;
import xyz.mobi.employeehelpdesk.repository.SkillRepository;
import xyz.mobi.employeehelpdesk.repository.SubCategoryRepository;
import xyz.mobi.employeehelpdesk.repository.SubCategorySkillRepository;
import xyz.mobi.employeehelpdesk.service.CurrentUserService;
import xyz.mobi.employeehelpdesk.service.DepartmentAuthorizationService;
import xyz.mobi.employeehelpdesk.service.SubCategorySkillAdminService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubCategorySkillAdminServiceImpl implements SubCategorySkillAdminService {

    private final SubCategoryRepository subCategoryRepository;
    private final SkillRepository skillRepository;
    private final SubCategorySkillRepository subCategorySkillRepository;
    private final CurrentUserService currentUserService;
    private final DepartmentAuthorizationService departmentAuthorizationService;

    @Override
    @Transactional
    public SubCategorySkillResponse assignSkillToSubCategory(Long subCategoryId, SubCategorySkillRequest request) {
        SubCategory subCategory = subCategoryRepository.findById(subCategoryId)
                .orElseThrow(() -> new ResourceNotFoundException("SubCategory not found with id: " + subCategoryId));

        checkDepartmentAccess(subCategory.getCategory().getDepartment().getId());

        if (!Boolean.TRUE.equals(subCategory.getCategory().getIsActive())) {
            throw new BadRequestException("Category is inactive: " + subCategory.getCategory().getName());
        }

        if (!Boolean.TRUE.equals(subCategory.getCategory().getDepartment().getIsActive())) {
            throw new BadRequestException("Department is inactive: " + subCategory.getCategory().getDepartment().getName());
        }

        Skill skill = skillRepository.findById(request.skillId())
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found with id: " + request.skillId()));

        if (!Boolean.TRUE.equals(skill.getIsActive())) {
            throw new BadRequestException("Skill is inactive: " + skill.getName());
        }

        if (subCategorySkillRepository.existsBySubCategoryIdAndSkillId(subCategoryId, request.skillId())) {
            throw new BadRequestException("Skill is already assigned to this subcategory: " + skill.getName());
        }

        SubCategorySkill subCategorySkill = new SubCategorySkill();
        subCategorySkill.setSubCategory(subCategory);
        subCategorySkill.setSkill(skill);

        SubCategorySkill saved = subCategorySkillRepository.save(subCategorySkill);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubCategorySkillResponse> getSubCategorySkills(Long subCategoryId) {
        SubCategory subCategory = subCategoryRepository.findById(subCategoryId)
                .orElseThrow(() -> new ResourceNotFoundException("SubCategory not found with id: " + subCategoryId));

        checkDepartmentAccess(subCategory.getCategory().getDepartment().getId());

        return subCategorySkillRepository.findBySubCategoryId(subCategoryId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void removeSkillFromSubCategory(Long subCategoryId, Long skillId) {
        SubCategory subCategory = subCategoryRepository.findById(subCategoryId)
                .orElseThrow(() -> new ResourceNotFoundException("SubCategory not found with id: " + subCategoryId));

        checkDepartmentAccess(subCategory.getCategory().getDepartment().getId());

        if (!skillRepository.existsById(skillId)) {
            throw new ResourceNotFoundException("Skill not found with id: " + skillId);
        }

        SubCategorySkill subCategorySkill = subCategorySkillRepository.findBySubCategoryIdAndSkillId(subCategoryId, skillId)
                .orElseThrow(() -> new ResourceNotFoundException("SubCategory skill mapping not found for subcategory " + subCategoryId + " and skill " + skillId));

        subCategorySkillRepository.delete(subCategorySkill);
    }

    private void checkDepartmentAccess(Long departmentId) {
        UserRole role = currentUserService.getCurrentUserRole();
        if (role == UserRole.ADMIN) {
            return;
        }
        if (role == UserRole.MANAGER) {
            Long currentEmpId = currentUserService.getCurrentEmployeeId();
            if (!departmentAuthorizationService.isManagerOfDepartment(currentEmpId, departmentId)) {
                throw new AccessDeniedException("Access denied: You are not a manager of department " + departmentId);
            }
            return;
        }
        throw new AccessDeniedException("Access denied: Insufficient permissions");
    }

    private SubCategorySkillResponse toResponse(SubCategorySkill scs) {
        return SubCategorySkillResponse.builder()
                .id(scs.getId())
                .subCategoryId(scs.getSubCategory().getId())
                .subCategoryName(scs.getSubCategory().getName())
                .skillId(scs.getSkill().getId())
                .skillName(scs.getSkill().getName())
                .createdAt(scs.getCreatedAt())
                .build();
    }
}
