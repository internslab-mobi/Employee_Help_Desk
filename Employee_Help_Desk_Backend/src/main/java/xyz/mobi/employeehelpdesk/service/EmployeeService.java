package xyz.mobi.employeehelpdesk.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import xyz.mobi.employeehelpdesk.dto.employee.CreateEmployeeRequestDTO;
import xyz.mobi.employeehelpdesk.dto.employee.EmployeeResponseDTO;
import xyz.mobi.employeehelpdesk.dto.employee.ProfilePhotoDTO;
import xyz.mobi.employeehelpdesk.dto.employee.UpdateEmployeeRequestDTO;
import xyz.mobi.employeehelpdesk.entity.enums.EmploymentStatus;
import xyz.mobi.employeehelpdesk.entity.enums.UserRole;
import xyz.mobi.employeehelpdesk.security.UserPrincipal;

import java.io.IOException;

public interface EmployeeService {

    EmployeeResponseDTO createEmployee(CreateEmployeeRequestDTO request, UserPrincipal userPrincipal);

    Page<EmployeeResponseDTO> getAllEmployees(Pageable pageable, UserPrincipal userPrincipal);

    Page<EmployeeResponseDTO> searchEmployees(
            String search,
            Long departmentId,
            EmploymentStatus status,
            UserRole role,
            Pageable pageable,
            UserPrincipal userPrincipal);

    EmployeeResponseDTO getEmployeeById(Long id, UserPrincipal userPrincipal);

    EmployeeResponseDTO getCurrentEmployeeProfile(UserPrincipal userPrincipal);

    void deactivateEmployee(Long id);

    EmployeeResponseDTO updateEmployee(
            Long employeeId,
            UpdateEmployeeRequestDTO request,
            MultipartFile profileImage,
            UserPrincipal userPrincipal
    ) throws IOException;

    ProfilePhotoDTO getCurrentEmployeePhoto(UserPrincipal userPrincipal);

    ProfilePhotoDTO getEmployeePhoto(Long id);
}