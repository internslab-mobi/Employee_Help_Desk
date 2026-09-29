package com.divya.helpdesk.service;

import com.divya.helpdesk.dto.user.*;

import java.util.List;

public interface EmployeeService {

    EmployeeUpdateResponse updateEntireEmployee(Long employeeId, UpdateEmployeeRequest request);

    EmployeeUpdateResponse patchEmployee(Long employeeId, EmployeePatchRequest request);

    EmployeeUpdateResponse updateMyProfile(Long employeeId, UpdateProfileRequest request);

    EmployeeResponse createEmployee(CreateEmployeeRequest request);

    EmployeeResponse getEmployee(Long employeeId);

    EmployeeResponse getMyProfile(Long employeeId);
}