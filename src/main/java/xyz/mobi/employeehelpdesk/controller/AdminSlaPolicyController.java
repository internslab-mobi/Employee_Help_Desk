package xyz.mobi.employeehelpdesk.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import xyz.mobi.employeehelpdesk.dto.slapolicy.CreateSlaPolicyRequest;
import xyz.mobi.employeehelpdesk.dto.slapolicy.SlaPolicyResponse;
import xyz.mobi.employeehelpdesk.dto.slapolicy.UpdateSlaPolicyRequest;
import xyz.mobi.employeehelpdesk.service.SlaPolicyAdminService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/sla-policies")
@RequiredArgsConstructor
public class AdminSlaPolicyController {

    private final SlaPolicyAdminService slaPolicyAdminService;

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PostMapping
    public ResponseEntity<SlaPolicyResponse> createSlaPolicy(
            @Valid @RequestBody CreateSlaPolicyRequest request
    ) {
        SlaPolicyResponse response = slaPolicyAdminService.createSlaPolicy(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT', 'EMPLOYEE')")
    @GetMapping
    public ResponseEntity<List<SlaPolicyResponse>> getAllSlaPolicies(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long subCategoryId,
            @RequestParam(required = false) Boolean activeOnly
    ) {
        List<SlaPolicyResponse> response = slaPolicyAdminService.getAllSlaPolicies(departmentId, subCategoryId, activeOnly);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT', 'EMPLOYEE')")
    @GetMapping("/{id}")
    public ResponseEntity<SlaPolicyResponse> getSlaPolicyById(@PathVariable Long id) {
        SlaPolicyResponse response = slaPolicyAdminService.getSlaPolicyById(id);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PutMapping("/{id}")
    public ResponseEntity<SlaPolicyResponse> updateSlaPolicy(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSlaPolicyRequest request
    ) {
        SlaPolicyResponse response = slaPolicyAdminService.updateSlaPolicy(id, request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSlaPolicy(@PathVariable Long id) {
        slaPolicyAdminService.deleteSlaPolicy(id);
        return ResponseEntity.noContent().build();
    }
}
