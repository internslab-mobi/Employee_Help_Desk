package com.divya.helpdesk.service;

import com.divya.helpdesk.dto.user.*;

public interface EmployeeService {

    UpdateEmployeeResponseDTO updateEntireEmployee(Long employeeId, UpdateEmployeeRequestDTO request);

    UpdateEmployeeResponseDTO patchEmployee(Long employeeId, PatchEmployeeRequestDTO request);

    UpdateEmployeeResponseDTO updateMyProfile(Long employeeId, UpdateProfileRequestDTO request);

    CreateEmployeeResponseDTO createEmployee(CreateEmployeeRequestDTO request);

    CreateEmployeeResponseDTO getEmployee(Long employeeId);

    CreateEmployeeResponseDTO getMyProfile(Long employeeId);
}