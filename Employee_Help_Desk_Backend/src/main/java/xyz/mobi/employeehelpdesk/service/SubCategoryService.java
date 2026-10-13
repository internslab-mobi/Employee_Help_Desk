package xyz.mobi.employeehelpdesk.service;

import xyz.mobi.employeehelpdesk.dto.subcategory.SubCategoryResponseDTO;

import java.util.List;

public interface SubCategoryService {

    List<SubCategoryResponseDTO> getSubCategories(Long categoryId);

    SubCategoryResponseDTO getSubCategoryById(Long id);
}
