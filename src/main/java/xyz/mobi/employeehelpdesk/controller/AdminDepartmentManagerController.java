package xyz.mobi.employeehelpdesk.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import xyz.mobi.employeehelpdesk.dto.manager.AssignManagerRequest;
import xyz.mobi.employeehelpdesk.dto.manager.ManagerAssignmentResponse;
import xyz.mobi.employeehelpdesk.service.DepartmentManagerAdminService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AdminDepartmentManagerController {

    private final DepartmentManagerAdminService departmentManagerAdminService;

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PostMapping("/api/admin/departments/{departmentId}/managers")
    public ResponseEntity<ManagerAssignmentResponse> assignManager(
            @PathVariable Long departmentId,
            @Valid @RequestBody AssignManagerRequest request
    ) {
        ManagerAssignmentResponse response = departmentManagerAdminService.assignManager(departmentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping("/api/admin/departments/{departmentId}/managers")
    public ResponseEntity<List<ManagerAssignmentResponse>> getDepartmentManagers(
            @PathVariable Long departmentId
    ) {
        List<ManagerAssignmentResponse> response = departmentManagerAdminService.getDepartmentManagers(departmentId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping("/api/admin/managers/{employeeId}/departments")
    public ResponseEntity<List<ManagerAssignmentResponse>> getManagerDepartments(
            @PathVariable Long employeeId
    ) {
        List<ManagerAssignmentResponse> response = departmentManagerAdminService.getManagerDepartments(employeeId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @DeleteMapping("/api/admin/departments/{departmentId}/managers/{employeeId}")
    public ResponseEntity<Void> unassignManager(
            @PathVariable Long departmentId,
            @PathVariable Long employeeId
    ) {
        departmentManagerAdminService.unassignManager(departmentId, employeeId);
        return ResponseEntity.noContent().build();
    }
}
