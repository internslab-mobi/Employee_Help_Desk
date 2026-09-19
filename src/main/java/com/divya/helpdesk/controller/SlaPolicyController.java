package com.divya.helpdesk.controller;

import com.divya.helpdesk.dto.request.SlaPolicyRequest;
import com.divya.helpdesk.dto.response.SlaPolicyResponse;
import com.divya.helpdesk.service.SlaPolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sla-policies")
@RequiredArgsConstructor
public class SlaPolicyController {

    private final SlaPolicyService slaPolicyService;

    @PostMapping
    public ResponseEntity<SlaPolicyResponse> createOrUpdatePolicy(@Valid @RequestBody SlaPolicyRequest request) {
        SlaPolicyResponse response = slaPolicyService.createOrUpdatePolicy(request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<List<SlaPolicyResponse>> getAllPolicies(@RequestParam(required = false) Long departmentId) {
        return ResponseEntity.ok(slaPolicyService.getAllPolicies(departmentId));
    }

    @GetMapping("/department/{departmentId}/subcategory/{subCategoryId}")
    public ResponseEntity<SlaPolicyResponse> getPolicyByDeptAndSubCategory(
            @PathVariable Long departmentId,
            @PathVariable Long subCategoryId) {
        return ResponseEntity.ok(slaPolicyService.getPolicyByDeptAndSubCategory(departmentId, subCategoryId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SlaPolicyResponse> getPolicyById(@PathVariable Long id) {
        return ResponseEntity.ok(slaPolicyService.getPolicyById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePolicy(@PathVariable Long id) {
        slaPolicyService.deletePolicy(id);
        return ResponseEntity.noContent().build();
    }
}
