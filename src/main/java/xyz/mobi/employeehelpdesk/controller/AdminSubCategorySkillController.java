package xyz.mobi.employeehelpdesk.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import xyz.mobi.employeehelpdesk.dto.subcategoryskill.SubCategorySkillRequest;
import xyz.mobi.employeehelpdesk.dto.subcategoryskill.SubCategorySkillResponse;
import xyz.mobi.employeehelpdesk.service.SubCategorySkillAdminService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AdminSubCategorySkillController {

    private final SubCategorySkillAdminService subCategorySkillAdminService;

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PostMapping("/api/admin/subcategories/{subCategoryId}/skills")
    public ResponseEntity<SubCategorySkillResponse> assignSkillToSubCategory(
            @PathVariable Long subCategoryId,
            @Valid @RequestBody SubCategorySkillRequest request
    ) {
        SubCategorySkillResponse response = subCategorySkillAdminService.assignSkillToSubCategory(subCategoryId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping("/api/admin/subcategories/{subCategoryId}/skills")
    public ResponseEntity<List<SubCategorySkillResponse>> getSubCategorySkills(
            @PathVariable Long subCategoryId
    ) {
        List<SubCategorySkillResponse> response = subCategorySkillAdminService.getSubCategorySkills(subCategoryId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @DeleteMapping("/api/admin/subcategories/{subCategoryId}/skills/{skillId}")
    public ResponseEntity<Void> removeSkillFromSubCategory(
            @PathVariable Long subCategoryId,
            @PathVariable Long skillId
    ) {
        subCategorySkillAdminService.removeSkillFromSubCategory(subCategoryId, skillId);
        return ResponseEntity.noContent().build();
    }
}
