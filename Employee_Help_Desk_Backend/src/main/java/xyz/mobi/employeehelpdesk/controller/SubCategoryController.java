package xyz.mobi.employeehelpdesk.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import xyz.mobi.employeehelpdesk.dto.subcategory.CreateSubCategoryRequestDTO;
import xyz.mobi.employeehelpdesk.dto.subcategory.SubCategoryResponseDTO;
import xyz.mobi.employeehelpdesk.service.SubCategoryService;

import java.util.List;

@Tag(name = "SubCategories", description = "Master Data: SubCategory retrieval endpoints")
@RestController
@RequestMapping("/helpdesk/sub-categories")
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

    @Operation(summary = "Create a new subcategory")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<SubCategoryResponseDTO> createSubCategory(
            @Valid @RequestBody CreateSubCategoryRequestDTO request
    ) {
        SubCategoryResponseDTO subCategory = subCategoryService.createSubCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(subCategory);
    }
}
