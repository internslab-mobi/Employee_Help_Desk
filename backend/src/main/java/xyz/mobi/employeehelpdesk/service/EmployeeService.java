package xyz.mobi.employeehelpdesk.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import xyz.mobi.employeehelpdesk.dto.employee.CreateEmployeeRequestDTO;
import xyz.mobi.employeehelpdesk.dto.employee.EmployeeCreateResponseDTO;
import xyz.mobi.employeehelpdesk.dto.employee.EmployeeResponseDTO;
import xyz.mobi.employeehelpdesk.dto.employee.UpdateEmployeeRequestDTO;
import xyz.mobi.employeehelpdesk.entity.enums.EmploymentStatus;
import xyz.mobi.employeehelpdesk.entity.enums.UserRole;

import java.io.IOException;

public interface EmployeeService {
    @Transactional
    EmployeeCreateResponseDTO createEmployee(CreateEmployeeRequestDTO request);

    @Transactional(readOnly = true)
    Page<EmployeeResponseDTO> getAllEmployees(Pageable pageable);

    @Transactional(readOnly = true)
    Page<EmployeeResponseDTO> searchEmployees(
            String search,
            Long departmentId,
            EmploymentStatus status,
            UserRole role,
            Pageable pageable);

    @Transactional(readOnly = true)
    EmployeeResponseDTO getEmployeeById(Long id);

    @Transactional
    void deactivateEmployee(Long id);

    @Transactional
    EmployeeResponseDTO updateEmployee(
            Long employeeId,
            UpdateEmployeeRequestDTO request,
            MultipartFile profileImage
    ) throws IOException;
}
