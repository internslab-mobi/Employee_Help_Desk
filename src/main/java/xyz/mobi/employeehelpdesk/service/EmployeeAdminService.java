package xyz.mobi.employeehelpdesk.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import xyz.mobi.employeehelpdesk.dto.employee.CreateEmployeeRequest;
import xyz.mobi.employeehelpdesk.dto.employee.EmployeeAdminResponse;
import xyz.mobi.employeehelpdesk.dto.employee.ResetPasswordRequest;
import xyz.mobi.employeehelpdesk.dto.employee.UpdateEmployeeRequest;
import xyz.mobi.employeehelpdesk.entity.enums.EmploymentStatus;
import xyz.mobi.employeehelpdesk.entity.enums.UserRole;

public interface EmployeeAdminService {

    EmployeeAdminResponse createEmployee(CreateEmployeeRequest request);

    Page<EmployeeAdminResponse> getAllEmployees(Long departmentId, EmploymentStatus status, UserRole role, Pageable pageable);

    EmployeeAdminResponse getEmployeeById(Long id);

    EmployeeAdminResponse updateEmployee(Long id, UpdateEmployeeRequest request);

    EmployeeAdminResponse updateEmployeeStatus(Long id, Boolean enabled, EmploymentStatus status);

    void resetPassword(Long id, ResetPasswordRequest request);

    void deleteEmployee(Long id);
}
