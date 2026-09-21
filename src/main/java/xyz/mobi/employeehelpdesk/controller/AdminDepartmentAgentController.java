package xyz.mobi.employeehelpdesk.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import xyz.mobi.employeehelpdesk.dto.agent.AgentAssignmentResponse;
import xyz.mobi.employeehelpdesk.dto.agent.AssignAgentRequest;
import xyz.mobi.employeehelpdesk.service.DepartmentAgentAdminService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AdminDepartmentAgentController {

    private final DepartmentAgentAdminService departmentAgentAdminService;

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PostMapping("/api/admin/departments/{departmentId}/agents")
    public ResponseEntity<AgentAssignmentResponse> assignAgent(
            @PathVariable Long departmentId,
            @Valid @RequestBody AssignAgentRequest request
    ) {
        AgentAssignmentResponse response = departmentAgentAdminService.assignAgent(departmentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping("/api/admin/departments/{departmentId}/agents")
    public ResponseEntity<List<AgentAssignmentResponse>> getDepartmentAgents(
            @PathVariable Long departmentId
    ) {
        List<AgentAssignmentResponse> response = departmentAgentAdminService.getDepartmentAgents(departmentId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping("/api/admin/agents/{employeeId}/departments")
    public ResponseEntity<List<AgentAssignmentResponse>> getAgentDepartments(
            @PathVariable Long employeeId
    ) {
        List<AgentAssignmentResponse> response = departmentAgentAdminService.getAgentDepartments(employeeId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @DeleteMapping("/api/admin/departments/{departmentId}/agents/{employeeId}")
    public ResponseEntity<Void> unassignAgent(
            @PathVariable Long departmentId,
            @PathVariable Long employeeId
    ) {
        departmentAgentAdminService.unassignAgent(departmentId, employeeId);
        return ResponseEntity.noContent().build();
    }
}
