package xyz.mobi.employeehelpdesk.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import xyz.mobi.employeehelpdesk.dto.department.CategoryResponseDTO;
import xyz.mobi.employeehelpdesk.dto.department.DepartmentResponseDTO;
import xyz.mobi.employeehelpdesk.dto.department.SubCategoryResponseDTO;
import xyz.mobi.employeehelpdesk.repository.CategoryRepository;
import xyz.mobi.employeehelpdesk.repository.DepartmentRepository;
import xyz.mobi.employeehelpdesk.repository.SubCategoryRepository;

import java.util.List;

@Tag(name = "Departments & Categories", description = "Endpoints for department, category, and subcategory lookups")
@RestController
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentRepository departmentRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;

    @Operation(summary = "Get all active departments")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/departments")
    public ResponseEntity<List<DepartmentResponseDTO>> getActiveDepartments() {
        List<DepartmentResponseDTO> list = departmentRepository.findByIsActiveTrue()
                .stream()
                .map(d -> new DepartmentResponseDTO(d.getId(), d.getCode(), d.getName(), d.getDescription()))
                .toList();
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "Get active categories for a department")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/departments/{departmentId}/categories")
    public ResponseEntity<List<CategoryResponseDTO>> getCategoriesByDepartment(@PathVariable Long departmentId) {
        List<CategoryResponseDTO> list = categoryRepository.findByDepartmentIdAndIsActiveTrue(departmentId)
                .stream()
                .map(c -> new CategoryResponseDTO(c.getId(), c.getDepartment().getId(), c.getName(), c.getDescription()))
                .toList();
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "Get active subcategories for a category")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/categories/{categoryId}/subcategories")
    public ResponseEntity<List<SubCategoryResponseDTO>> getSubCategoriesByCategory(@PathVariable Long categoryId) {
        List<SubCategoryResponseDTO> list = subCategoryRepository.findByCategoryIdAndIsActiveTrue(categoryId)
                .stream()
                .map(s -> new SubCategoryResponseDTO(s.getId(), s.getCategory().getId(), s.getName(), s.getDescription(), s.getPriority()))
                .toList();
        return ResponseEntity.ok(list);
    }
}
