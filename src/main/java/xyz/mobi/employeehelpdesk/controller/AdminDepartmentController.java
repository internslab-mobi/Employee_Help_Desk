package xyz.mobi.employeehelpdesk.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import xyz.mobi.employeehelpdesk.dto.department.CreateDepartmentRequest;
import xyz.mobi.employeehelpdesk.dto.department.DepartmentResponse;
import xyz.mobi.employeehelpdesk.dto.department.UpdateDepartmentRequest;
import xyz.mobi.employeehelpdesk.service.DepartmentAdminService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/departments")
@RequiredArgsConstructor
public class AdminDepartmentController {

    private final DepartmentAdminService departmentAdminService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<DepartmentResponse> createDepartment(
            @Valid @RequestBody CreateDepartmentRequest request
    ) {
        DepartmentResponse response = departmentAdminService.createDepartment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT', 'EMPLOYEE')")
    @GetMapping
    public ResponseEntity<List<DepartmentResponse>> getAllDepartments(
            @RequestParam(required = false) Boolean activeOnly
    ) {
        List<DepartmentResponse> response = departmentAdminService.getAllDepartments(activeOnly);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT', 'EMPLOYEE')")
    @GetMapping("/{id}")
    public ResponseEntity<DepartmentResponse> getDepartmentById(@PathVariable Long id) {
        DepartmentResponse response = departmentAdminService.getDepartmentById(id);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<DepartmentResponse> updateDepartment(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDepartmentRequest request
    ) {
        DepartmentResponse response = departmentAdminService.updateDepartment(id, request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDepartment(@PathVariable Long id) {
        departmentAdminService.deleteDepartment(id);
        return ResponseEntity.noContent().build();
    }
}
