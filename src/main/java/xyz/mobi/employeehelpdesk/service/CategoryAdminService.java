package xyz.mobi.employeehelpdesk.service;

import xyz.mobi.employeehelpdesk.dto.category.CategoryResponse;
import xyz.mobi.employeehelpdesk.dto.category.CreateCategoryRequest;
import xyz.mobi.employeehelpdesk.dto.category.UpdateCategoryRequest;

import java.util.List;

public interface CategoryAdminService {

    CategoryResponse createCategory(CreateCategoryRequest request);

    List<CategoryResponse> getAllCategories(Long departmentId, Boolean activeOnly);

    CategoryResponse getCategoryById(Long id);

    CategoryResponse updateCategory(Long id, UpdateCategoryRequest request);

    void deleteCategory(Long id);
}
