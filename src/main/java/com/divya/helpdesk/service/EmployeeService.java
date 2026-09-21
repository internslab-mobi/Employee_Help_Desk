package com.divya.helpdesk.service;

import com.divya.helpdesk.dto.request.EmployeeCreateRequest;
import com.divya.helpdesk.dto.request.EmployeeUpdateRequest;
import com.divya.helpdesk.dto.response.EmployeeResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface EmployeeService {
    EmployeeResponse createEmployee(EmployeeCreateRequest request);
    EmployeeResponse updateEmployee(Long id, EmployeeUpdateRequest request);
    EmployeeResponse getEmployeeById(Long id);
    List<EmployeeResponse> getAllEmployees(Long departmentId);
    void uploadProfileImage(Long employeeId, MultipartFile file);
    byte[] getProfileImage(Long employeeId);
    String getProfileImageType(Long employeeId);
}
