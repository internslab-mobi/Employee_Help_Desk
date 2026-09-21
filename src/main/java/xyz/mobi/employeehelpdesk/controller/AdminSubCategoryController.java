package xyz.mobi.employeehelpdesk.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import xyz.mobi.employeehelpdesk.dto.subcategory.CreateSubCategoryRequest;
import xyz.mobi.employeehelpdesk.dto.subcategory.SubCategoryResponse;
import xyz.mobi.employeehelpdesk.dto.subcategory.UpdateSubCategoryRequest;
import xyz.mobi.employeehelpdesk.service.SubCategoryAdminService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/subcategories")
@RequiredArgsConstructor
public class AdminSubCategoryController {

    private final SubCategoryAdminService subCategoryAdminService;

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PostMapping
    public ResponseEntity<SubCategoryResponse> createSubCategory(
            @Valid @RequestBody CreateSubCategoryRequest request
    ) {
        SubCategoryResponse response = subCategoryAdminService.createSubCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT', 'EMPLOYEE')")
    @GetMapping
    public ResponseEntity<List<SubCategoryResponse>> getAllSubCategories(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean activeOnly
    ) {
        List<SubCategoryResponse> response = subCategoryAdminService.getAllSubCategories(categoryId, activeOnly);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT', 'EMPLOYEE')")
    @GetMapping("/{id}")
    public ResponseEntity<SubCategoryResponse> getSubCategoryById(@PathVariable Long id) {
        SubCategoryResponse response = subCategoryAdminService.getSubCategoryById(id);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PutMapping("/{id}")
    public ResponseEntity<SubCategoryResponse> updateSubCategory(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSubCategoryRequest request
    ) {
        SubCategoryResponse response = subCategoryAdminService.updateSubCategory(id, request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubCategory(@PathVariable Long id) {
        subCategoryAdminService.deleteSubCategory(id);
        return ResponseEntity.noContent().build();
    }
}
