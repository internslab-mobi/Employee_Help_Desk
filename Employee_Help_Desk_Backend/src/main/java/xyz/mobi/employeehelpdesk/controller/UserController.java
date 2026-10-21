package xyz.mobi.employeehelpdesk.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import xyz.mobi.employeehelpdesk.dto.employee.CreateEmployeeRequestDTO;
import xyz.mobi.employeehelpdesk.dto.employee.EmployeeResponseDTO;
import xyz.mobi.employeehelpdesk.dto.employee.UpdateEmployeeRequestDTO;
import xyz.mobi.employeehelpdesk.entity.enums.EmploymentStatus;
import xyz.mobi.employeehelpdesk.entity.enums.UserRole;
import xyz.mobi.employeehelpdesk.service.EmployeeService;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/helpdesk/user")
@RequiredArgsConstructor
public class UserController {

    private final EmployeeService employeeService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<EmployeeResponseDTO> createEmployee(
            @Valid @RequestBody CreateEmployeeRequestDTO request
    ) {
        EmployeeResponseDTO response = employeeService.createEmployee(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping
    public ResponseEntity<Page<EmployeeResponseDTO>> getAllEmployees(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<EmployeeResponseDTO> response = employeeService.getAllEmployees(pageable);

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping("/search")
    public ResponseEntity<Page<EmployeeResponseDTO>> searchEmployees(
            @RequestParam String search,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) EmploymentStatus status,
            @RequestParam(required = false) UserRole role,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<EmployeeResponseDTO> response = employeeService.searchEmployees(search, departmentId, status, role, pageable);

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN', 'MANAGER', 'AGENT')")
    @GetMapping("/me")
    public ResponseEntity<EmployeeResponseDTO> getCurrentUserProfile() {
        EmployeeResponseDTO response = employeeService.getCurrentEmployeeProfile();
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    @PatchMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EmployeeResponseDTO> updateCurrentUserProfile(
            @Valid @RequestPart(value = "request", required = true) UpdateEmployeeRequestDTO request,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage
    ) throws IOException {
        EmployeeResponseDTO response = employeeService.updateEmployee(
                null,
                request,
                profileImage
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN', 'MANAGER', 'AGENT')")
    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponseDTO> getEmployeeById(
            @PathVariable Long id
    ) {
        EmployeeResponseDTO response = employeeService.getEmployeeById(id);

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}/deactivate")
    public ResponseEntity<Map<String, String>> deactivateEmployee(
            @PathVariable Long id
    ) {
        employeeService.deactivateEmployee(id);

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .body(Map.of("message", "Employee deactivated successfully"));
    }

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')")
    @PatchMapping(value = "/{employeeId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EmployeeResponseDTO> updateEmployee(
            @PathVariable Long employeeId,
            @Valid @RequestPart(value = "request", required = true) UpdateEmployeeRequestDTO request,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage
    ) throws IOException {
        EmployeeResponseDTO response = employeeService.updateEmployee(
                employeeId,
                request,
                profileImage
        );
        return ResponseEntity.ok(response);
    }
}