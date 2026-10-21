package xyz.mobi.employeehelpdesk.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.mobi.employeehelpdesk.dto.department.DepartmentResponseDTO;
import xyz.mobi.employeehelpdesk.service.DepartmentService;

import java.util.List;

@Tag(name = "Departments", description = "Master Data: Department retrieval endpoints")
@RestController
@RequestMapping("/helpdesk/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @Operation(summary = "Get all active departments")
    @GetMapping
    public ResponseEntity<List<DepartmentResponseDTO>> getAllDepartments() {
        List<DepartmentResponseDTO> departments = departmentService.getAllDepartments();
        return ResponseEntity.ok(departments);
    }

    @Operation(summary = "Get department by ID")
    @GetMapping("/{id}")
    public ResponseEntity<DepartmentResponseDTO> getDepartmentById(@PathVariable Long id) {
        DepartmentResponseDTO department = departmentService.getDepartmentById(id);
        return ResponseEntity.ok(department);
    }

    @Operation(summary = "Create a new department")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @org.springframework.web.bind.annotation.PostMapping
    public ResponseEntity<DepartmentResponseDTO> createDepartment(
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody xyz.mobi.employeehelpdesk.dto.department.CreateDepartmentRequestDTO request
    ) {
        DepartmentResponseDTO department = departmentService.createDepartment(request);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(department);
    }
}
