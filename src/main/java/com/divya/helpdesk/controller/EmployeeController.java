package com.divya.helpdesk.controller;

import com.divya.helpdesk.dto.user.*;
import com.divya.helpdesk.security.CurrentUserService;
import com.divya.helpdesk.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private final CurrentUserService currentUserService;

    // ADMIN - UPDATE EMPLOYEE
    @PutMapping("/{employeeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmployeeUpdateResponse> updateEntireEmployee(@PathVariable Long employeeId, @Valid @RequestBody UpdateEmployeeRequest request) {

        return ResponseEntity.ok(employeeService.updateEntireEmployee(employeeId, request));
    }

    // ADMIN - PATCH EMPLOYEE
    @PatchMapping("/{employeeId}")
    public ResponseEntity<EmployeeUpdateResponse> patchEmployee(@PathVariable Long employeeId, @RequestBody EmployeePatchRequest request){

        return ResponseEntity.ok(employeeService.patchEmployee(employeeId, request));
    }

    // CURRENT USER - PATCH PROFILE
    @PatchMapping("/me")
    public ResponseEntity<EmployeeUpdateResponse> updateMyProfile(@RequestBody UpdateProfileRequest request) {
        Long employeeId = currentUserService.getEmployeeId();

        return ResponseEntity.ok(employeeService.updateMyProfile(employeeId, request));
    }

    // ADMIN - CREATE EMPLOYEE
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmployeeResponse> createEmployee(@Valid @RequestBody CreateEmployeeRequest request) {
        return new ResponseEntity<>(employeeService.createEmployee(request), HttpStatus.CREATED);
    }

    // ADMIN - GET EMPLOYEE BY ID
    @GetMapping("/{employeeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmployeeResponse> getEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(employeeService.getEmployee(employeeId));
    }

    // CURRENT USER - GET PROFILE
    @GetMapping("/me")
    public ResponseEntity<EmployeeResponse> getMyProfile() {
        Long employeeId = currentUserService.getEmployeeId();
        return ResponseEntity.ok(employeeService.getMyProfile(employeeId));
    }

}