package com.divya.helpdesk.service.impl;

import com.divya.helpdesk.dto.request.SubCategoryCreateRequest;
import com.divya.helpdesk.dto.request.SubCategoryPatchRequest;
import com.divya.helpdesk.dto.request.SubCategoryUpdateRequest;
import com.divya.helpdesk.dto.response.SubCategoryResponse;
import com.divya.helpdesk.entity.HDCategory;
import com.divya.helpdesk.entity.HDEmployee;
import com.divya.helpdesk.entity.HDSubCategory;
import com.divya.helpdesk.exception.BadRequestException;
import com.divya.helpdesk.exception.ResourceNotFoundException;
import com.divya.helpdesk.mapper.SubCategoryMapper;
import com.divya.helpdesk.repository.HDCategoryRepository;
import com.divya.helpdesk.repository.HDEmployeeRepository;
import com.divya.helpdesk.repository.HDSubCategoryRepository;
import com.divya.helpdesk.service.SubCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SubCategoryServiceImpl implements SubCategoryService {

    private final HDSubCategoryRepository subCategoryRepository;
    private final HDCategoryRepository categoryRepository;
    private final HDEmployeeRepository employeeRepository;
    private final SubCategoryMapper subCategoryMapper;

    @Override
    public SubCategoryResponse createSubCategory(SubCategoryCreateRequest request) {
        HDCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));

        if (subCategoryRepository.existsByCategoryIdAndName(request.getCategoryId(), request.getName())) {
            throw new BadRequestException("SubCategory with name '" + request.getName() + "' already exists in category " + category.getName());
        }

        HDSubCategory subCategory = subCategoryMapper.toEntity(request);
        subCategory.setCategory(category);

        if (request.getCreatedById() != null) {
            HDEmployee createdBy = employeeRepository.findById(request.getCreatedById())
                    .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + request.getCreatedById()));
            subCategory.setCreatedBy(createdBy);
        }

        HDSubCategory saved = subCategoryRepository.save(subCategory);
        return subCategoryMapper.toResponse(saved);
    }

    @Override
    public SubCategoryResponse updateSubCategory(Long id, SubCategoryUpdateRequest request) {
        HDSubCategory subCategory = subCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SubCategory not found with id: " + id));

        if (!subCategory.getName().equalsIgnoreCase(request.getName()) &&
                subCategoryRepository.existsByCategoryIdAndName(subCategory.getCategory().getId(), request.getName())) {
            throw new BadRequestException("SubCategory with name '" + request.getName() + "' already exists in category " + subCategory.getCategory().getName());
        }

        subCategoryMapper.updateEntity(subCategory, request);
        HDSubCategory saved = subCategoryRepository.save(subCategory);
        return subCategoryMapper.toResponse(saved);
    }

    @Override
    public SubCategoryResponse patchSubCategory(Long id, SubCategoryPatchRequest request) {
        HDSubCategory subCategory = subCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SubCategory not found with id: " + id));

        if (request.getName() != null) {
            if (!subCategory.getName().equalsIgnoreCase(request.getName()) &&
                    subCategoryRepository.existsByCategoryIdAndName(subCategory.getCategory().getId(), request.getName())) {
                throw new BadRequestException("SubCategory with name '" + request.getName() + "' already exists in category " + subCategory.getCategory().getName());
            }
            subCategory.setName(request.getName());
        }
        if (request.getDescription() != null) {
            subCategory.setDescription(request.getDescription());
        }
        if (request.getPriority() != null) {
            subCategory.setPriority(request.getPriority());
        }
        if (request.getIsActive() != null) {
            subCategory.setIsActive(request.getIsActive());
        }

        HDSubCategory saved = subCategoryRepository.save(subCategory);
        return subCategoryMapper.toResponse(saved);
    }

    @Override
    public void deleteSubCategory(Long id) {
        if (!subCategoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("SubCategory not found with id: " + id);
        }
        subCategoryRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public SubCategoryResponse getSubCategoryById(Long id) {
        HDSubCategory subCategory = subCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SubCategory not found with id: " + id));
        return subCategoryMapper.toResponse(subCategory);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubCategoryResponse> getSubCategoriesByCategory(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category not found with id: " + categoryId);
        }
        return subCategoryRepository.findByCategoryId(categoryId).stream()
                .map(subCategoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubCategoryResponse> getAllSubCategories() {
        return subCategoryRepository.findAll().stream()
                .map(subCategoryMapper::toResponse)
                .collect(Collectors.toList());
    }
}
