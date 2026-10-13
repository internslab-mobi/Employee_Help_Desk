package xyz.mobi.employeehelpdesk.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import xyz.mobi.employeehelpdesk.dto.subcategory.SubCategoryResponseDTO;
import xyz.mobi.employeehelpdesk.service.SubCategoryService;

import java.util.List;

@Tag(name = "SubCategories", description = "Master Data: SubCategory retrieval endpoints")
@RestController
@RequestMapping("/sub-categories")
@RequiredArgsConstructor
public class SubCategoryController {

    private final SubCategoryService subCategoryService;

    @Operation(summary = "Get all active subcategories, optionally filtered by category ID")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    @GetMapping
    public ResponseEntity<List<SubCategoryResponseDTO>> getSubCategories(
            @RequestParam(required = false) Long categoryId
    ) {
        List<SubCategoryResponseDTO> subCategories = subCategoryService.getSubCategories(categoryId);
        return ResponseEntity.ok(subCategories);
    }

    @Operation(summary = "Get subcategory by ID")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<SubCategoryResponseDTO> getSubCategoryById(@PathVariable Long id) {
        SubCategoryResponseDTO subCategory = subCategoryService.getSubCategoryById(id);
        return ResponseEntity.ok(subCategory);
    }
}
