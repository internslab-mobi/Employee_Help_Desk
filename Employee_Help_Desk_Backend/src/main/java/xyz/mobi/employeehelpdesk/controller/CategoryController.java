package xyz.mobi.employeehelpdesk.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import xyz.mobi.employeehelpdesk.dto.category.CategoryResponseDTO;
import xyz.mobi.employeehelpdesk.service.CategoryService;

import java.util.List;

@Tag(name = "Categories", description = "Master Data: Category retrieval endpoints")
@RestController
@RequestMapping("/helpdesk/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "Get all active categories, optionally filtered by department ID")
    @GetMapping
    public ResponseEntity<List<CategoryResponseDTO>> getAllCategories(
            @RequestParam(required = false) Long departmentId
    ) {
        List<CategoryResponseDTO> categories = categoryService.getAllCategories(departmentId);
        return ResponseEntity.ok(categories);
    }

    @Operation(summary = "Get category by ID")
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponseDTO> getCategoryById(@PathVariable Long id) {
        CategoryResponseDTO category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(category);
    }

    @Operation(summary = "Create a new category")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<CategoryResponseDTO> createCategory(
            @jakarta.validation.Valid @RequestBody xyz.mobi.employeehelpdesk.dto.category.CreateCategoryRequestDTO request
    ) {
        CategoryResponseDTO category = categoryService.createCategory(request);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(category);
    }
}
