package com.example.helpdesk.controller;

import com.example.helpdesk.dto.request.CreateEmployeeRequest;
import com.example.helpdesk.dto.request.CreateUserRequest;
import com.example.helpdesk.dto.response.EmployeeResponse;
import com.example.helpdesk.entity.Category;
import com.example.helpdesk.entity.Department;
import com.example.helpdesk.entity.Employee;
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
@Tag(name = "User Management", description = "Employee, Agent, Manager, Department, Category, Sub-category, and Skill management")
public class UserController {

    private final UserService userService;

    // ==================== USER ACCOUNT CREATION ====================

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create user (consolidated)", description = "🔐 Access: ADMIN only. Consolidated endpoint to create EMPLOYEE, AGENT, or MANAGER accounts. Generates temporary password and OTP for first-login flow.")
    public ResponseEntity<EmployeeResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }


    // ==================== DEPARTMENT ENDPOINTS ====================

    @PostMapping("/departments")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create department", description = "🔐 Access: ADMIN only.")
    public ResponseEntity<Department> createDepartment(@Valid @RequestBody Department department) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createDepartment(department));
    }


    // ==================== CATEGORY ENDPOINTS ====================

    @PostMapping("/categories")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create category", description = "🔐 Access: ADMIN only.")
    public ResponseEntity<Category> createCategory(@Valid @RequestBody Category category) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createCategory(category));
    }


    // ==================== SUB-CATEGORY ENDPOINTS ====================

    @PostMapping("/sub-categories")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create sub-category", description = "🔐 Access: ADMIN only.")
    public ResponseEntity<SubCategory> createSubCategory(@Valid @RequestBody SubCategory subCategory) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createSubCategory(subCategory));
    }


    // ==================== SKILL ENDPOINTS ====================

    @PostMapping("/skills")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create skill", description = "🔐 Access: ADMIN only.")
    public ResponseEntity<Skill> createSkill(@Valid @RequestBody Skill skill) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createSkill(skill));
    }

}
