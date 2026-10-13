package xyz.mobi.employeehelpdesk.service;

import xyz.mobi.employeehelpdesk.dto.category.CategoryResponseDTO;

import java.util.List;

public interface CategoryService {

    List<CategoryResponseDTO> getAllCategories(Long departmentId);

    CategoryResponseDTO getCategoryById(Long id);
}
