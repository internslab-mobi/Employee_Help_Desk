package xyz.mobi.employeehelpdesk.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.mobi.employeehelpdesk.dto.category.CategoryResponseDTO;
import xyz.mobi.employeehelpdesk.entity.Category;
import xyz.mobi.employeehelpdesk.exception.ResourceNotFoundException;
import xyz.mobi.employeehelpdesk.repository.CategoryRepository;
import xyz.mobi.employeehelpdesk.repository.DepartmentRepository;
import xyz.mobi.employeehelpdesk.service.CategoryService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final DepartmentRepository departmentRepository;

    @Override
    public List<CategoryResponseDTO> getAllCategories(Long departmentId) {
        log.debug("Fetching categories with departmentId: {}", departmentId);
        List<Category> categories;

        if (departmentId != null) {
            departmentRepository.findByIdAndIsActiveTrue(departmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + departmentId));
            categories = categoryRepository.findByDepartmentIdAndIsActiveTrueOrderByNameAsc(departmentId);
        } else {
            categories = categoryRepository.findByIsActiveTrueOrderByNameAsc();
        }

        return categories.stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public CategoryResponseDTO getCategoryById(Long id) {
        log.debug("Fetching category by id: {}", id);
        return categoryRepository.findByIdAndIsActiveTrue(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }

    private CategoryResponseDTO toResponse(Category category) {
        return CategoryResponseDTO.builder()
                .id(category.getId())
                .departmentId(category.getDepartment() != null ? category.getDepartment().getId() : null)
                .departmentName(category.getDepartment() != null ? category.getDepartment().getName() : null)
                .name(category.getName())
                .description(category.getDescription())
                .isActive(category.getIsActive())
                .build();
    }
}
