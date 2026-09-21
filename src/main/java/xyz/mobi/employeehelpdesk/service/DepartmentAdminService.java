package xyz.mobi.employeehelpdesk.service;

import xyz.mobi.employeehelpdesk.dto.department.CreateDepartmentRequest;
import xyz.mobi.employeehelpdesk.dto.department.DepartmentResponse;
import xyz.mobi.employeehelpdesk.dto.department.UpdateDepartmentRequest;

import java.util.List;

public interface DepartmentAdminService {

    DepartmentResponse createDepartment(CreateDepartmentRequest request);

    List<DepartmentResponse> getAllDepartments(Boolean activeOnly);

    DepartmentResponse getDepartmentById(Long id);

    DepartmentResponse updateDepartment(Long id, UpdateDepartmentRequest request);

    void deleteDepartment(Long id);
}
