package xyz.mobi.employeehelpdesk.service;

import xyz.mobi.employeehelpdesk.dto.subcategory.CreateSubCategoryRequest;
import xyz.mobi.employeehelpdesk.dto.subcategory.SubCategoryResponse;
import xyz.mobi.employeehelpdesk.dto.subcategory.UpdateSubCategoryRequest;

import java.util.List;

public interface SubCategoryAdminService {

    SubCategoryResponse createSubCategory(CreateSubCategoryRequest request);

    List<SubCategoryResponse> getAllSubCategories(Long categoryId, Boolean activeOnly);

    SubCategoryResponse getSubCategoryById(Long id);

    SubCategoryResponse updateSubCategory(Long id, UpdateSubCategoryRequest request);

    void deleteSubCategory(Long id);
}
