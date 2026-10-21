package xyz.mobi.employeehelpdesk.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.mobi.employeehelpdesk.dto.subcategory.SubCategoryResponseDTO;
import xyz.mobi.employeehelpdesk.entity.Category;
import xyz.mobi.employeehelpdesk.entity.Department;
import xyz.mobi.employeehelpdesk.entity.Employee;
import xyz.mobi.employeehelpdesk.entity.SubCategory;
import xyz.mobi.employeehelpdesk.exception.ResourceNotFoundException;
import xyz.mobi.employeehelpdesk.repository.CategoryRepository;
import xyz.mobi.employeehelpdesk.repository.SubCategoryRepository;
import xyz.mobi.employeehelpdesk.service.SubCategoryService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubCategoryServiceImpl implements SubCategoryService {

    private final SubCategoryRepository subCategoryRepository;
    private final CategoryRepository categoryRepository;
    private final xyz.mobi.employeehelpdesk.repository.SubCategorySkillRepository subCategorySkillRepository;
    private final xyz.mobi.employeehelpdesk.repository.SkillRepository skillRepository;

    @Override
    public List<SubCategoryResponseDTO> getSubCategories(Long categoryId) {
        log.debug("Fetching subcategories with categoryId: {}", categoryId);
        List<SubCategory> subCategories;

        if (categoryId != null) {
            categoryRepository.findByIdAndIsActiveTrue(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
            subCategories = subCategoryRepository.findActiveByCategoryId(categoryId);
        } else {
            subCategories = subCategoryRepository.findAllActive();
        }

        return subCategories.stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public SubCategoryResponseDTO getSubCategoryById(Long id) {
        log.debug("Fetching subcategory by id: {}", id);
        return subCategoryRepository.findByIdAndIsActiveTrue(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Subcategory not found with id: " + id));
    }

    @Override
    @Transactional
    public SubCategoryResponseDTO createSubCategory(xyz.mobi.employeehelpdesk.dto.subcategory.CreateSubCategoryRequestDTO request) {
        log.info("Creating new subcategory: categoryId={}, name={}", request.categoryId(), request.name());
        Category category = categoryRepository.findByIdAndIsActiveTrue(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.categoryId()));

        SubCategory sc = new SubCategory();
        sc.setCategory(category);
        sc.setName(request.name().trim());
        sc.setDescription(request.description() != null ? request.description().trim() : null);
        sc.setPriority(request.priority() != null ? request.priority() : xyz.mobi.employeehelpdesk.entity.enums.Priority.MEDIUM);
        sc.setIsActive(true);

        SubCategory saved = subCategoryRepository.save(sc);

        // Associate skills if provided
        if (request.skillIds() != null && !request.skillIds().isEmpty()) {
            for (Long skillId : request.skillIds()) {
                skillRepository.findById(skillId).ifPresent(skill -> {
                    xyz.mobi.employeehelpdesk.entity.SubCategorySkill scs = new xyz.mobi.employeehelpdesk.entity.SubCategorySkill();
                    scs.setSubCategory(saved);
                    scs.setSkill(skill);
                    subCategorySkillRepository.save(scs);
                });
            }
        }

        return toResponse(saved);
    }

    private SubCategoryResponseDTO toResponse(SubCategory sc) {
        Category cat = sc.getCategory();
        Department dept = (cat != null) ? cat.getDepartment() : null;
        Employee creator = sc.getCreatedBy();

        String creatorName = null;
        if (creator != null) {
            creatorName = (creator.getFirstName() + (creator.getLastName() != null ? " " + creator.getLastName() : "")).trim();
        }

        List<xyz.mobi.employeehelpdesk.entity.SubCategorySkill> skills = subCategorySkillRepository.findBySubCategoryId(sc.getId());
        List<Long> skillIds = (skills != null) ? skills.stream().map(s -> s.getSkill().getId()).toList() : java.util.Collections.emptyList();
        List<String> skillNames = (skills != null) ? skills.stream().map(s -> s.getSkill().getName()).toList() : java.util.Collections.emptyList();

        return SubCategoryResponseDTO.builder()
                .id(sc.getId())
                .categoryId(cat != null ? cat.getId() : null)
                .categoryName(cat != null ? cat.getName() : null)
                .departmentId(dept != null ? dept.getId() : null)
                .departmentName(dept != null ? dept.getName() : null)
                .name(sc.getName())
                .description(sc.getDescription())
                .priority(sc.getPriority())
                .isActive(sc.getIsActive())
                .createdById(creator != null ? creator.getId() : null)
                .createdByName(creatorName)
                .skillIds(skillIds)
                .skillNames(skillNames)
                .build();
    }
}
