package com.divya.helpdesk.controller;

import com.divya.helpdesk.dto.request.*;
import com.divya.helpdesk.dto.response.DepartmentAgentResponse;
import com.divya.helpdesk.dto.response.DepartmentManagerResponse;
import com.divya.helpdesk.dto.response.DepartmentResponse;
import com.divya.helpdesk.dto.response.EmployeeResponse;
import com.divya.helpdesk.service.DepartmentService;
import com.divya.helpdesk.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;
    private final EmployeeService employeeService;

    // --- Department Endpoints ---

    @PostMapping
    public ResponseEntity<DepartmentResponse> createDepartment(@Valid @RequestBody DepartmentCreateRequest request) {
        DepartmentResponse response = departmentService.createDepartment(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<DepartmentResponse>> getAllDepartments() {
        return ResponseEntity.ok(departmentService.getAllDepartments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DepartmentResponse> getDepartmentById(@PathVariable Long id) {
        return ResponseEntity.ok(departmentService.getDepartmentById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DepartmentResponse> updateDepartment(@PathVariable Long id, @Valid @RequestBody DepartmentUpdateRequest request) {
        return ResponseEntity.ok(departmentService.updateDepartment(id, request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<DepartmentResponse> patchDepartment(@PathVariable Long id, @RequestBody DepartmentPatchRequest request) {
        return ResponseEntity.ok(departmentService.patchDepartment(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.noContent().build();
    }

    // --- Agent Endpoints ---

    @PostMapping("/{deptId}/agents")
    public ResponseEntity<DepartmentAgentResponse> addAgent(
            @PathVariable Long deptId,
            @Valid @RequestBody DepartmentAgentRequest request) {
        DepartmentAgentResponse response = departmentService.addAgent(deptId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{deptId}/agents")
    public ResponseEntity<List<DepartmentAgentResponse>> getAgentsByDepartment(@PathVariable Long deptId) {
        return ResponseEntity.ok(departmentService.getAgentsByDepartment(deptId));
    }

    // --- Manager Endpoints ---

    @PostMapping("/{deptId}/managers")
    public ResponseEntity<DepartmentManagerResponse> addManager(
            @PathVariable Long deptId,
            @Valid @RequestBody DepartmentManagerRequest request) {
        DepartmentManagerResponse response = departmentService.addManager(deptId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{deptId}/managers")
    public ResponseEntity<List<DepartmentManagerResponse>> getManagersByDepartment(@PathVariable Long deptId) {
        return ResponseEntity.ok(departmentService.getManagersByDepartment(deptId));
    }

    // --- Employee Endpoints ---

    @PostMapping("/employees")
    public ResponseEntity<EmployeeResponse> createEmployee(@Valid @RequestBody EmployeeCreateRequest request) {
        EmployeeResponse response = employeeService.createEmployee(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/{deptId}/employees")
    public ResponseEntity<EmployeeResponse> createEmployeeInDepartment(
            @PathVariable Long deptId,
            @Valid @RequestBody EmployeeCreateRequest request) {
        request.setDepartmentId(deptId);
        EmployeeResponse response = employeeService.createEmployee(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/employees")
    public ResponseEntity<List<EmployeeResponse>> getAllEmployees(@RequestParam(required = false) Long departmentId) {
        return ResponseEntity.ok(employeeService.getAllEmployees(departmentId));
    }

    @GetMapping("/{deptId}/employees")
    public ResponseEntity<List<EmployeeResponse>> getEmployeesByDepartment(@PathVariable Long deptId) {
        return ResponseEntity.ok(employeeService.getAllEmployees(deptId));
    }

    @GetMapping("/employees/{id}")
    public ResponseEntity<EmployeeResponse> getEmployeeById(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getEmployeeById(id));
    }

    @PutMapping("/employees/{id}")
    public ResponseEntity<EmployeeResponse> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeUpdateRequest request) {
        return ResponseEntity.ok(employeeService.updateEmployee(id, request));
    }

    @PostMapping("/employees/{id}/profile-image")
    public ResponseEntity<String> uploadProfileImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        employeeService.uploadProfileImage(id, file);
        return ResponseEntity.ok("Profile image uploaded successfully");
    }

    @GetMapping("/employees/{id}/profile-image")
    public ResponseEntity<byte[]> getProfileImage(@PathVariable Long id) {
        byte[] imageBytes = employeeService.getProfileImage(id);
        String contentType = employeeService.getProfileImageType(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType != null ? contentType : MediaType.IMAGE_JPEG_VALUE)
                .body(imageBytes);
    }
}
