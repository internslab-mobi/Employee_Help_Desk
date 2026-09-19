package com.divya.helpdesk.service;

import com.divya.helpdesk.dto.request.SubCategoryCreateRequest;
import com.divya.helpdesk.dto.request.SubCategoryPatchRequest;
import com.divya.helpdesk.dto.request.SubCategoryUpdateRequest;
import com.divya.helpdesk.dto.response.SubCategoryResponse;

import java.util.List;

public interface SubCategoryService {
    SubCategoryResponse createSubCategory(SubCategoryCreateRequest request);
    SubCategoryResponse updateSubCategory(Long id, SubCategoryUpdateRequest request);
    SubCategoryResponse patchSubCategory(Long id, SubCategoryPatchRequest request);
    void deleteSubCategory(Long id);
    SubCategoryResponse getSubCategoryById(Long id);
    List<SubCategoryResponse> getSubCategoriesByCategory(Long categoryId);
    List<SubCategoryResponse> getAllSubCategories();
}
