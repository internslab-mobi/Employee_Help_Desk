package com.divya.helpdesk.service.impl;

import com.divya.helpdesk.dto.request.CategoryCreateRequest;
import com.divya.helpdesk.dto.request.CategoryPatchRequest;
import com.divya.helpdesk.dto.request.CategoryUpdateRequest;
import com.divya.helpdesk.dto.response.CategoryResponse;
import com.divya.helpdesk.entity.HDCategory;
import com.divya.helpdesk.entity.HDDepartment;
import com.divya.helpdesk.entity.HDEmployee;
import com.divya.helpdesk.exception.BadRequestException;
import com.divya.helpdesk.exception.ResourceNotFoundException;
import com.divya.helpdesk.mapper.CategoryMapper;
import com.divya.helpdesk.repository.HDCategoryRepository;
import com.divya.helpdesk.repository.HDDepartmentRepository;
import com.divya.helpdesk.repository.HDEmployeeRepository;
import com.divya.helpdesk.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final HDCategoryRepository categoryRepository;
    private final HDDepartmentRepository departmentRepository;
    private final HDEmployeeRepository employeeRepository;
    private final CategoryMapper categoryMapper;

    @Override
    public CategoryResponse createCategory(CategoryCreateRequest request) {
        HDDepartment department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.getDepartmentId()));

        if (categoryRepository.existsByDepartmentIdAndName(request.getDepartmentId(), request.getName())) {
            throw new BadRequestException("Category with name '" + request.getName() + "' already exists in department " + department.getName());
        }

        HDCategory category = categoryMapper.toEntity(request);
        category.setDepartment(department);

        if (request.getCreatedById() != null) {
            HDEmployee createdBy = employeeRepository.findById(request.getCreatedById())
                    .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + request.getCreatedById()));
            category.setCreatedBy(createdBy);
        }

        HDCategory saved = categoryRepository.save(category);
        return categoryMapper.toResponse(saved);
    }

    @Override
    public CategoryResponse updateCategory(Long id, CategoryUpdateRequest request) {
        HDCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        if (!category.getName().equalsIgnoreCase(request.getName()) &&
                categoryRepository.existsByDepartmentIdAndName(category.getDepartment().getId(), request.getName())) {
            throw new BadRequestException("Category with name '" + request.getName() + "' already exists in department " + category.getDepartment().getName());
        }

        categoryMapper.updateEntity(category, request);
        HDCategory saved = categoryRepository.save(category);
        return categoryMapper.toResponse(saved);
    }

    @Override
    public CategoryResponse patchCategory(Long id, CategoryPatchRequest request) {
        HDCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        if (request.getName() != null) {
            if (!category.getName().equalsIgnoreCase(request.getName()) &&
                    categoryRepository.existsByDepartmentIdAndName(category.getDepartment().getId(), request.getName())) {
                throw new BadRequestException("Category with name '" + request.getName() + "' already exists in department " + category.getDepartment().getName());
            }
            category.setName(request.getName());
        }
        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }
        if (request.getIsActive() != null) {
            category.setIsActive(request.getIsActive());
        }

        HDCategory saved = categoryRepository.save(category);
        return categoryMapper.toResponse(saved);
    }

    @Override
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category not found with id: " + id);
        }
        categoryRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        HDCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        return categoryMapper.toResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoriesByDepartment(Long departmentId) {
        if (!departmentRepository.existsById(departmentId)) {
            throw new ResourceNotFoundException("Department not found with id: " + departmentId);
        }
        return categoryRepository.findByDepartmentId(departmentId).stream()
                .map(categoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(categoryMapper::toResponse)
                .collect(Collectors.toList());
    }
}
