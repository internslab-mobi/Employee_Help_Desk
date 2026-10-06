package xyz.mobi.employeehelpdesk.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import xyz.mobi.employeehelpdesk.dto.employee.CreateEmployeeRequestDTO;
import xyz.mobi.employeehelpdesk.dto.employee.EmployeeCreateResponseDTO;
import xyz.mobi.employeehelpdesk.dto.employee.EmployeePatchRequestDTO;
import xyz.mobi.employeehelpdesk.dto.employee.EmployeeResponseDTO;
import xyz.mobi.employeehelpdesk.entity.enums.EmploymentStatus;
import xyz.mobi.employeehelpdesk.entity.enums.UserRole;

public interface EmployeeService {

    EmployeeCreateResponseDTO createEmployee(CreateEmployeeRequestDTO request);

    Page<EmployeeResponseDTO> getAllEmployees(Pageable pageable);

    Page<EmployeeResponseDTO> searchEmployees(
            String search,
            Long departmentId,
            EmploymentStatus status,
            UserRole role,
            Pageable pageable
    );

    EmployeeResponseDTO getEmployeeById(Long id);

    EmployeeResponseDTO patchEmployee(Long id, EmployeePatchRequestDTO request);

    void deactivateEmployee(Long id);
}