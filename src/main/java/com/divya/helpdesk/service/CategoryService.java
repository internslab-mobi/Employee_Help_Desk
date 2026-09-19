package com.divya.helpdesk.service;

import com.divya.helpdesk.dto.request.CategoryCreateRequest;
import com.divya.helpdesk.dto.request.CategoryPatchRequest;
import com.divya.helpdesk.dto.request.CategoryUpdateRequest;
import com.divya.helpdesk.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {
    CategoryResponse createCategory(CategoryCreateRequest request);
    CategoryResponse updateCategory(Long id, CategoryUpdateRequest request);
    CategoryResponse patchCategory(Long id, CategoryPatchRequest request);
    void deleteCategory(Long id);
    CategoryResponse getCategoryById(Long id);
    List<CategoryResponse> getCategoriesByDepartment(Long departmentId);
    List<CategoryResponse> getAllCategories();
}
