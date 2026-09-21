package xyz.mobi.employeehelpdesk.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.mobi.employeehelpdesk.dto.category.CategoryResponse;
import xyz.mobi.employeehelpdesk.dto.category.CreateCategoryRequest;
import xyz.mobi.employeehelpdesk.dto.category.UpdateCategoryRequest;
import xyz.mobi.employeehelpdesk.entity.Category;
import xyz.mobi.employeehelpdesk.entity.Department;
import xyz.mobi.employeehelpdesk.entity.Employee;
import xyz.mobi.employeehelpdesk.entity.enums.UserRole;
import xyz.mobi.employeehelpdesk.exception.BadRequestException;
import xyz.mobi.employeehelpdesk.exception.ResourceNotFoundException;
import xyz.mobi.employeehelpdesk.repository.CategoryRepository;
import xyz.mobi.employeehelpdesk.repository.DepartmentRepository;
import xyz.mobi.employeehelpdesk.repository.EmployeeRepository;
import xyz.mobi.employeehelpdesk.repository.SubCategoryRepository;
import xyz.mobi.employeehelpdesk.repository.TicketRepository;
import xyz.mobi.employeehelpdesk.service.CategoryAdminService;
import xyz.mobi.employeehelpdesk.service.CurrentUserService;
import xyz.mobi.employeehelpdesk.service.DepartmentAuthorizationService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryAdminServiceImpl implements CategoryAdminService {

    private final CategoryRepository categoryRepository;
    private final DepartmentRepository departmentRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final TicketRepository ticketRepository;
    private final EmployeeRepository employeeRepository;
    private final CurrentUserService currentUserService;
    private final DepartmentAuthorizationService departmentAuthorizationService;

    @Override
    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        checkDepartmentAccess(request.departmentId());

        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.departmentId()));

        if (!Boolean.TRUE.equals(department.getIsActive())) {
            throw new BadRequestException("Department is inactive: " + department.getName());
        }

        if (categoryRepository.existsByDepartmentIdAndName(request.departmentId(), request.name().trim())) {
            throw new BadRequestException("Category name already exists in this department: " + request.name());
        }

        Employee createdBy = null;
        Long currentEmpId = currentUserService.getCurrentEmployeeId();
        if (currentEmpId != null) {
            createdBy = employeeRepository.findById(currentEmpId).orElse(null);
        }

        Category category = new Category();
        category.setDepartment(department);
        category.setName(request.name().trim());
        category.setDescription(request.description() != null ? request.description().trim() : null);
        category.setIsActive(true);
        category.setCreatedBy(createdBy);

        Category saved = categoryRepository.save(category);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories(Long departmentId, Boolean activeOnly) {
        List<Category> list;
        if (departmentId != null) {
            list = Boolean.TRUE.equals(activeOnly)
                    ? categoryRepository.findByDepartmentIdAndIsActiveTrue(departmentId)
                    : categoryRepository.findByDepartmentId(departmentId);
        } else {
            list = Boolean.TRUE.equals(activeOnly)
                    ? categoryRepository.findByIsActiveTrue()
                    : categoryRepository.findAll();
        }

        return list.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        return toResponse(category);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, UpdateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        checkDepartmentAccess(category.getDepartment().getId());

        if (categoryRepository.existsByDepartmentIdAndNameAndIdNot(category.getDepartment().getId(), request.name().trim(), id)) {
            throw new BadRequestException("Category name already exists in this department: " + request.name());
        }

        category.setName(request.name().trim());
        category.setDescription(request.description() != null ? request.description().trim() : null);
        category.setIsActive(request.isActive());

        Category updated = categoryRepository.save(category);
        return toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        checkDepartmentAccess(category.getDepartment().getId());

        if (!subCategoryRepository.findByCategoryId(id).isEmpty()) {
            throw new BadRequestException("Cannot delete category with existing subcategories. Deactivate the category instead.");
        }

        if (ticketRepository.existsByCategoryId(id)) {
            throw new BadRequestException("Cannot delete category with existing tickets. Deactivate the category instead.");
        }

        try {
            categoryRepository.delete(category);
        } catch (Exception ex) {
            throw new BadRequestException("Cannot delete category due to existing references. Deactivate the category instead.");
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

    private CategoryResponse toResponse(Category category) {
        String createdByName = null;
        Long createdById = null;
        if (category.getCreatedBy() != null) {
            createdById = category.getCreatedBy().getId();
            createdByName = category.getCreatedBy().getFirstName()
                    + (category.getCreatedBy().getLastName() != null ? " " + category.getCreatedBy().getLastName() : "");
        }

        return CategoryResponse.builder()
                .id(category.getId())
                .departmentId(category.getDepartment().getId())
                .departmentName(category.getDepartment().getName())
                .name(category.getName())
                .description(category.getDescription())
                .isActive(category.getIsActive())
                .createdById(createdById)
                .createdByName(createdByName)
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}
