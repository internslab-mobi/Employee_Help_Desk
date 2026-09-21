package xyz.mobi.employeehelpdesk.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import xyz.mobi.employeehelpdesk.dto.employee.CreateEmployeeRequest;
import xyz.mobi.employeehelpdesk.dto.employee.EmployeeAdminResponse;
import xyz.mobi.employeehelpdesk.dto.employee.ResetPasswordRequest;
import xyz.mobi.employeehelpdesk.dto.employee.UpdateEmployeeRequest;
import xyz.mobi.employeehelpdesk.entity.enums.EmploymentStatus;
import xyz.mobi.employeehelpdesk.entity.enums.UserRole;
import xyz.mobi.employeehelpdesk.service.EmployeeAdminService;

@RestController
@RequestMapping("/api/admin/employees")
@RequiredArgsConstructor
public class AdminEmployeeController {

    private final EmployeeAdminService employeeAdminService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<EmployeeAdminResponse> createEmployee(
            @Valid @RequestBody CreateEmployeeRequest request
    ) {
        EmployeeAdminResponse response = employeeAdminService.createEmployee(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<EmployeeAdminResponse>> getAllEmployees(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) EmploymentStatus status,
            @RequestParam(required = false) UserRole role,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<EmployeeAdminResponse> response = employeeAdminService.getAllEmployees(departmentId, status, role, pageable);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<EmployeeAdminResponse> getEmployeeById(@PathVariable Long id) {
        EmployeeAdminResponse response = employeeAdminService.getEmployeeById(id);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<EmployeeAdminResponse> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEmployeeRequest request
    ) {
        EmployeeAdminResponse response = employeeAdminService.updateEmployee(id, request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<EmployeeAdminResponse> updateEmployeeStatus(
            @PathVariable Long id,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) EmploymentStatus status
    ) {
        EmployeeAdminResponse response = employeeAdminService.updateEmployeeStatus(id, enabled, status);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/reset-password")
    public ResponseEntity<Void> resetPassword(
            @PathVariable Long id,
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        employeeAdminService.resetPassword(id, request);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
        employeeAdminService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }
}
