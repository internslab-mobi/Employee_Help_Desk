package com.divya.helpdesk.controller;

import com.divya.helpdesk.dto.user.*;
import com.divya.helpdesk.service.CurrentUserService;
import com.divya.helpdesk.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private final CurrentUserService currentUserService;

    // ADMIN - UPDATE EMPLOYEE
    @PutMapping("/{employeeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UpdateEmployeeResponseDTO> updateEntireEmployee(@PathVariable Long employeeId, @Valid @RequestBody UpdateEmployeeRequestDTO request) {

        return ResponseEntity.ok(employeeService.updateEntireEmployee(employeeId, request));
    }

    // ADMIN - PATCH EMPLOYEE
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{employeeId}")
    public ResponseEntity<UpdateEmployeeResponseDTO> patchEmployee(@PathVariable Long employeeId, @RequestBody PatchEmployeeRequestDTO request){

        return ResponseEntity.ok(employeeService.patchEmployee(employeeId, request));
    }

    // CURRENT USER - PATCH PROFILE
    @PatchMapping("/me")
    public ResponseEntity<UpdateEmployeeResponseDTO> updateMyProfile(@RequestBody UpdateProfileRequestDTO request) {
        Long employeeId = currentUserService.getEmployeeId();

        return ResponseEntity.ok(employeeService.updateMyProfile(employeeId, request));
    }

    // ADMIN - CREATE EMPLOYEE
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CreateEmployeeResponseDTO> createEmployee(@Valid @RequestBody CreateEmployeeRequestDTO request) {
        return new ResponseEntity<>(employeeService.createEmployee(request), HttpStatus.CREATED);
    }

    // ADMIN - GET EMPLOYEE BY ID
    @GetMapping("/{employeeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CreateEmployeeResponseDTO> getEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(employeeService.getEmployee(employeeId));
    }

    // CURRENT USER - GET PROFILE
    @GetMapping("/me")
    public ResponseEntity<CreateEmployeeResponseDTO> getMyProfile() {
        Long employeeId = currentUserService.getEmployeeId();
        return ResponseEntity.ok(employeeService.getMyProfile(employeeId));
    }

}