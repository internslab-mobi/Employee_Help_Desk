package xyz.mobi.employeehelpdesk.service;

import xyz.mobi.employeehelpdesk.dto.category.CategoryResponseDTO;
import xyz.mobi.employeehelpdesk.dto.category.CreateCategoryRequestDTO;

import java.util.List;

public interface CategoryService {

    List<CategoryResponseDTO> getAllCategories(Long departmentId);

    CategoryResponseDTO getCategoryById(Long id);

    CategoryResponseDTO createCategory(CreateCategoryRequestDTO request);
}
