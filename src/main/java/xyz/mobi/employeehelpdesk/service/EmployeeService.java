package xyz.mobi.employeehelpdesk.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import xyz.mobi.employeehelpdesk.dto.employee.CreateEmployeeRequest;
import xyz.mobi.employeehelpdesk.dto.employee.EmployeeCreateResponse;
import xyz.mobi.employeehelpdesk.dto.employee.EmployeeResponse;
import xyz.mobi.employeehelpdesk.entity.enums.EmploymentStatus;
import xyz.mobi.employeehelpdesk.entity.enums.UserRole;

public interface EmployeeService {

    EmployeeCreateResponse createEmployee(CreateEmployeeRequest request);

    Page<EmployeeResponse> getAllEmployees(Pageable pageable);

    Page<EmployeeResponse> searchEmployees(
            String search,
            Long departmentId,
            EmploymentStatus status,
            UserRole role,
            Pageable pageable
    );

    EmployeeResponse getEmployeeById(Long id);

    void deactivateEmployee(Long id);
}