package xyz.mobi.employeehelpdesk.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.mobi.employeehelpdesk.dto.subcategory.CreateSubCategoryRequest;
import xyz.mobi.employeehelpdesk.dto.subcategory.SubCategoryResponse;
import xyz.mobi.employeehelpdesk.dto.subcategory.UpdateSubCategoryRequest;
import xyz.mobi.employeehelpdesk.entity.Category;
import xyz.mobi.employeehelpdesk.entity.Employee;
import xyz.mobi.employeehelpdesk.entity.SubCategory;
import xyz.mobi.employeehelpdesk.entity.enums.UserRole;
import xyz.mobi.employeehelpdesk.exception.BadRequestException;
import xyz.mobi.employeehelpdesk.exception.ResourceNotFoundException;
import xyz.mobi.employeehelpdesk.repository.*;
import xyz.mobi.employeehelpdesk.service.CurrentUserService;
import xyz.mobi.employeehelpdesk.service.DepartmentAuthorizationService;
import xyz.mobi.employeehelpdesk.service.SubCategoryAdminService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubCategoryAdminServiceImpl implements SubCategoryAdminService {

    private final SubCategoryRepository subCategoryRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategorySkillRepository subCategorySkillRepository;
    private final TicketRepository ticketRepository;
    private final EmployeeRepository employeeRepository;
    private final CurrentUserService currentUserService;
    private final DepartmentAuthorizationService departmentAuthorizationService;

    @Override
    @Transactional
    public SubCategoryResponse createSubCategory(CreateSubCategoryRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.categoryId()));

        checkDepartmentAccess(category.getDepartment().getId());

        if (!Boolean.TRUE.equals(category.getIsActive())) {
            throw new BadRequestException("Category is inactive: " + category.getName());
        }

        if (!Boolean.TRUE.equals(category.getDepartment().getIsActive())) {
            throw new BadRequestException("Department is inactive: " + category.getDepartment().getName());
        }

        if (subCategoryRepository.existsByCategoryIdAndName(request.categoryId(), request.name().trim())) {
            throw new BadRequestException("SubCategory name already exists in this category: " + request.name());
        }

        Employee createdBy = null;
        Long currentEmpId = currentUserService.getCurrentEmployeeId();
        if (currentEmpId != null) {
            createdBy = employeeRepository.findById(currentEmpId).orElse(null);
        }

        SubCategory subCategory = new SubCategory();
        subCategory.setCategory(category);
        subCategory.setName(request.name().trim());
        subCategory.setDescription(request.description() != null ? request.description().trim() : null);
        subCategory.setPriority(request.priority());
        subCategory.setIsActive(true);
        subCategory.setCreatedBy(createdBy);

        SubCategory saved = subCategoryRepository.save(subCategory);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubCategoryResponse> getAllSubCategories(Long categoryId, Boolean activeOnly) {
        List<SubCategory> list;
        if (categoryId != null) {
            list = Boolean.TRUE.equals(activeOnly)
                    ? subCategoryRepository.findByCategoryIdAndIsActiveTrue(categoryId)
                    : subCategoryRepository.findByCategoryId(categoryId);
        } else {
            list = Boolean.TRUE.equals(activeOnly)
                    ? subCategoryRepository.findByIsActiveTrue()
                    : subCategoryRepository.findAll();
        }

        return list.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SubCategoryResponse getSubCategoryById(Long id) {
        SubCategory subCategory = subCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SubCategory not found with id: " + id));
        return toResponse(subCategory);
    }

    @Override
    @Transactional
    public SubCategoryResponse updateSubCategory(Long id, UpdateSubCategoryRequest request) {
        SubCategory subCategory = subCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SubCategory not found with id: " + id));

        checkDepartmentAccess(subCategory.getCategory().getDepartment().getId());

        if (subCategoryRepository.existsByCategoryIdAndNameAndIdNot(subCategory.getCategory().getId(), request.name().trim(), id)) {
            throw new BadRequestException("SubCategory name already exists in this category: " + request.name());
        }

        subCategory.setName(request.name().trim());
        subCategory.setDescription(request.description() != null ? request.description().trim() : null);
        subCategory.setPriority(request.priority());
        subCategory.setIsActive(request.isActive());

        SubCategory updated = subCategoryRepository.save(subCategory);
        return toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteSubCategory(Long id) {
        SubCategory subCategory = subCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SubCategory not found with id: " + id));

        checkDepartmentAccess(subCategory.getCategory().getDepartment().getId());

        if (ticketRepository.existsBySubCategoryId(id)) {
            throw new BadRequestException("Cannot delete subcategory with existing tickets. Deactivate the subcategory instead.");
        }

        if (!subCategorySkillRepository.findBySubCategoryId(id).isEmpty()) {
            throw new BadRequestException("Cannot delete subcategory with assigned skills. Deactivate the subcategory instead.");
        }

        try {
            subCategoryRepository.delete(subCategory);
        } catch (Exception ex) {
            throw new BadRequestException("Cannot delete subcategory due to existing references. Deactivate the subcategory instead.");
        }
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

    private SubCategoryResponse toResponse(SubCategory sc) {
        String createdByName = null;
        Long createdById = null;
        if (sc.getCreatedBy() != null) {
            createdById = sc.getCreatedBy().getId();
            createdByName = sc.getCreatedBy().getFirstName()
                    + (sc.getCreatedBy().getLastName() != null ? " " + sc.getCreatedBy().getLastName() : "");
        }

        return SubCategoryResponse.builder()
                .id(sc.getId())
                .categoryId(sc.getCategory().getId())
                .categoryName(sc.getCategory().getName())
                .departmentId(sc.getCategory().getDepartment().getId())
                .departmentName(sc.getCategory().getDepartment().getName())
                .name(sc.getName())
                .description(sc.getDescription())
                .priority(sc.getPriority())
                .isActive(sc.getIsActive())
                .createdById(createdById)
                .createdByName(createdByName)
                .createdAt(sc.getCreatedAt())
                .updatedAt(sc.getUpdatedAt())
                .build();
    }
}
