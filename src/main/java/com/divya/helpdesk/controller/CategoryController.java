package com.divya.helpdesk.controller;

import com.divya.helpdesk.dto.request.CategoryCreateRequest;
import com.divya.helpdesk.dto.request.CategoryPatchRequest;
import com.divya.helpdesk.dto.request.CategoryUpdateRequest;
import com.divya.helpdesk.dto.request.SubCategoryCreateRequest;
import com.divya.helpdesk.dto.request.SubCategoryPatchRequest;
import com.divya.helpdesk.dto.request.SubCategoryUpdateRequest;
import com.divya.helpdesk.dto.response.CategoryResponse;
import com.divya.helpdesk.dto.response.SubCategoryResponse;
import com.divya.helpdesk.service.CategoryService;
import com.divya.helpdesk.service.SubCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final SubCategoryService subCategoryService;

    // --- Category Endpoints ---

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryCreateRequest request) {
        CategoryResponse response = categoryService.createCategory(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories(@RequestParam(required = false) Long departmentId) {
        if (departmentId != null) {
            return ResponseEntity.ok(categoryService.getCategoriesByDepartment(departmentId));
        }
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    @GetMapping("/department/{departmentId}")
    public ResponseEntity<List<CategoryResponse>> getCategoriesByDepartment(@PathVariable Long departmentId) {
        return ResponseEntity.ok(categoryService.getCategoriesByDepartment(departmentId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryUpdateRequest request) {
        return ResponseEntity.ok(categoryService.updateCategory(id, request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CategoryResponse> patchCategory(@PathVariable Long id, @RequestBody CategoryPatchRequest request) {
        return ResponseEntity.ok(categoryService.patchCategory(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    // --- Sub-Category Endpoints ---

    @PostMapping("/subcategories")
    public ResponseEntity<SubCategoryResponse> createSubCategory(@Valid @RequestBody SubCategoryCreateRequest request) {
        SubCategoryResponse response = subCategoryService.createSubCategory(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/{categoryId}/subcategories")
    public ResponseEntity<SubCategoryResponse> createSubCategoryForCategory(@PathVariable Long categoryId, @Valid @RequestBody SubCategoryCreateRequest request) {
        request.setCategoryId(categoryId);
        SubCategoryResponse response = subCategoryService.createSubCategory(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/subcategories")
    public ResponseEntity<List<SubCategoryResponse>> getAllSubCategories(@RequestParam(required = false) Long categoryId) {
        if (categoryId != null) {
            return ResponseEntity.ok(subCategoryService.getSubCategoriesByCategory(categoryId));
        }
        return ResponseEntity.ok(subCategoryService.getAllSubCategories());
    }

    @GetMapping("/subcategories/{id}")
    public ResponseEntity<SubCategoryResponse> getSubCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(subCategoryService.getSubCategoryById(id));
    }

    @GetMapping("/{categoryId}/subcategories")
    public ResponseEntity<List<SubCategoryResponse>> getSubCategoriesByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(subCategoryService.getSubCategoriesByCategory(categoryId));
    }

    @PutMapping("/subcategories/{id}")
    public ResponseEntity<SubCategoryResponse> updateSubCategory(@PathVariable Long id, @Valid @RequestBody SubCategoryUpdateRequest request) {
        return ResponseEntity.ok(subCategoryService.updateSubCategory(id, request));
    }

    @PatchMapping("/subcategories/{id}")
    public ResponseEntity<SubCategoryResponse> patchSubCategory(@PathVariable Long id, @RequestBody SubCategoryPatchRequest request) {
        return ResponseEntity.ok(subCategoryService.patchSubCategory(id, request));
    }

    @DeleteMapping("/subcategories/{id}")
    public ResponseEntity<Void> deleteSubCategory(@PathVariable Long id) {
        subCategoryService.deleteSubCategory(id);
        return ResponseEntity.noContent().build();
    }
}
