package com.example.helpdesk.controller;

import com.example.helpdesk.dto.request.CreateEmployeeRequestDTO;
import com.example.helpdesk.dto.request.CreateUserRequestDTO;
import com.example.helpdesk.dto.response.EmployeeResponseDTO;
import com.example.helpdesk.entity.Category;
import com.example.helpdesk.entity.Department;
import com.example.helpdesk.entity.Skill;
import com.example.helpdesk.entity.SubCategory;
import com.example.helpdesk.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "User management endpoints")
public class UserController {

    private final UserService userService;

    // ==================== EMPLOYEES ====================

    @PostMapping("/employees")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create Employee", description = "🔒 Access: ADMIN only — create a new employee")
    public ResponseEntity<EmployeeResponseDTO> createEmployee(@Valid @RequestBody CreateEmployeeRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createEmployee(request));
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create User", description = "🔒 Access: ADMIN only — create a new user with specified role")
    public ResponseEntity<EmployeeResponseDTO> createUser(@Valid @RequestBody CreateUserRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    // ==================== AGENTS ====================

    @PostMapping("/agents")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create Agent", description = "🔒 Access: ADMIN only — create a new agent")
    public ResponseEntity<EmployeeResponseDTO> createAgent(@Valid @RequestBody CreateEmployeeRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createAgent(request));
    }

    // ==================== MANAGERS ====================

    @PostMapping("/managers")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create Manager", description = "🔒 Access: ADMIN only — create a new manager")
    public ResponseEntity<EmployeeResponseDTO> createManager(@Valid @RequestBody CreateEmployeeRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createManager(request));
    }

    // ==================== DEPARTMENTS ====================

    @PostMapping("/departments")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create Department", description = "🔒 Access: ADMIN only — create a new department")
    public ResponseEntity<Department> createDepartment(@RequestBody Department department) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createDepartment(department));
    }

    // ==================== CATEGORIES ====================

    @PostMapping("/categories")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create Category", description = "🔒 Access: ADMIN only — create a new category")
    public ResponseEntity<Category> createCategory(@RequestBody Category category) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createCategory(category));
    }

    // ==================== SUB-CATEGORIES ====================

    @PostMapping("/subcategories")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create Sub-Category", description = "🔒 Access: ADMIN only — create a new sub-category")
    public ResponseEntity<SubCategory> createSubCategory(@RequestBody SubCategory subCategory) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createSubCategory(subCategory));
    }

    // ==================== SKILLS ====================

    @PostMapping("/skills")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create Skill", description = "🔒 Access: ADMIN only — create a new skill")
    public ResponseEntity<Skill> createSkill(@RequestBody Skill skill) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createSkill(skill));
    }

    @PostMapping("/agents/{agentId}/skills/{skillId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign Skill to Agent", description = "🔒 Access: ADMIN only — assign skill to agent")
    public ResponseEntity<Void> assignSkillToAgent(@PathVariable Long agentId, @PathVariable Long skillId) {
        userService.assignSkillToAgent(agentId, skillId);
        return ResponseEntity.noContent().build();
    }
}
