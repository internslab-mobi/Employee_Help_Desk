package xyz.mobi.employeehelpdesk.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.mobi.employeehelpdesk.dto.manager.AssignManagerRequest;
import xyz.mobi.employeehelpdesk.dto.manager.ManagerAssignmentResponse;
import xyz.mobi.employeehelpdesk.entity.Department;
import xyz.mobi.employeehelpdesk.entity.DepartmentManager;
import xyz.mobi.employeehelpdesk.entity.Employee;
import xyz.mobi.employeehelpdesk.entity.enums.EmploymentStatus;
import xyz.mobi.employeehelpdesk.entity.enums.UserRole;
import xyz.mobi.employeehelpdesk.exception.BadRequestException;
import xyz.mobi.employeehelpdesk.exception.ResourceNotFoundException;
import xyz.mobi.employeehelpdesk.repository.DepartmentManagerRepository;
import xyz.mobi.employeehelpdesk.repository.DepartmentRepository;
import xyz.mobi.employeehelpdesk.repository.EmployeeRepository;
import xyz.mobi.employeehelpdesk.service.CurrentUserService;
import xyz.mobi.employeehelpdesk.service.DepartmentAuthorizationService;
import xyz.mobi.employeehelpdesk.service.DepartmentManagerAdminService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepartmentManagerAdminServiceImpl implements DepartmentManagerAdminService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentManagerRepository departmentManagerRepository;
    private final CurrentUserService currentUserService;
    private final DepartmentAuthorizationService departmentAuthorizationService;

    @Override
    @Transactional
    public ManagerAssignmentResponse assignManager(Long departmentId, AssignManagerRequest request) {
        checkDepartmentAccess(departmentId);

        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + departmentId));

        if (!Boolean.TRUE.equals(department.getIsActive())) {
            throw new BadRequestException("Department is inactive: " + department.getName());
        }

        Employee employee = employeeRepository.findById(request.employeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + request.employeeId()));

        if (!Boolean.TRUE.equals(employee.getEnabled())) {
            throw new BadRequestException("Employee is disabled: " + employee.getEmail());
        }

        if (employee.getEmploymentStatus() != EmploymentStatus.ACTIVE) {
            throw new BadRequestException("Employee is not eligible for assignment (employment status: " + employee.getEmploymentStatus() + ")");
        }

        if (employee.getRole() != UserRole.MANAGER) {
            throw new BadRequestException("Employee does not have ROLE_MANAGER (role: " + employee.getRole() + ")");
        }

        if (departmentManagerRepository.existsByEmployeeIdAndDepartmentId(request.employeeId(), departmentId)) {
            throw new BadRequestException("Employee is already assigned to this department as a manager");
        }

        boolean isPrimary = Boolean.TRUE.equals(request.isPrimary());

        if (isPrimary) {
            // Demote any existing primary manager for this department
            departmentManagerRepository.findByDepartmentIdAndIsPrimaryTrue(departmentId)
                    .ifPresent(existingPrimary -> {
                        existingPrimary.setIsPrimary(false);
                        departmentManagerRepository.save(existingPrimary);
                    });
        }

        DepartmentManager manager = new DepartmentManager();
        manager.setDepartment(department);
        manager.setEmployee(employee);
        manager.setIsPrimary(isPrimary);

        DepartmentManager saved = departmentManagerRepository.save(manager);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManagerAssignmentResponse> getDepartmentManagers(Long departmentId) {
        checkDepartmentAccess(departmentId);

        if (!departmentRepository.existsById(departmentId)) {
            throw new ResourceNotFoundException("Department not found with id: " + departmentId);
        }

        return departmentManagerRepository.findByDepartmentId(departmentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManagerAssignmentResponse> getManagerDepartments(Long employeeId) {
        checkAdminOrManagerRole();

        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employee not found with id: " + employeeId);
        }

        return departmentManagerRepository.findByEmployeeId(employeeId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void unassignManager(Long departmentId, Long employeeId) {
        checkDepartmentAccess(departmentId);

        if (!departmentRepository.existsById(departmentId)) {
            throw new ResourceNotFoundException("Department not found with id: " + departmentId);
        }

        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employee not found with id: " + employeeId);
        }

        DepartmentManager manager = departmentManagerRepository.findByDepartmentIdAndEmployeeId(departmentId, employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager assignment not found for department " + departmentId + " and employee " + employeeId));

        if (Boolean.TRUE.equals(manager.getIsPrimary())) {
            List<DepartmentManager> allManagers = departmentManagerRepository.findByDepartmentId(departmentId);
            if (allManagers.size() > 1) {
                throw new BadRequestException("Cannot unassign primary manager while other managers exist. Designate another manager as primary first.");
            }
        }

        departmentManagerRepository.delete(manager);
    }

    private void checkDepartmentAccess(Long departmentId) {
        UserRole role = currentUserService.getCurrentUserRole();
        if (role == UserRole.ADMIN) {
            return;
        }
        if (role == UserRole.MANAGER) {
            Long currentEmpId = currentUserService.getCurrentEmployeeId();
            if (!departmentAuthorizationService.isManagerOfDepartment(currentEmpId, departmentId)) {
                throw new AccessDeniedException("Access denied: You are not a manager of department " + departmentId);
            }
            return;
        }
        throw new AccessDeniedException("Access denied: Insufficient permissions");
    }

    private void checkAdminOrManagerRole() {
        UserRole role = currentUserService.getCurrentUserRole();
        if (role != UserRole.ADMIN && role != UserRole.MANAGER) {
            throw new AccessDeniedException("Access denied: Insufficient permissions");
        }
    }

    private ManagerAssignmentResponse toResponse(DepartmentManager dm) {
        String employeeName = dm.getEmployee().getFirstName()
                + (dm.getEmployee().getLastName() != null ? " " + dm.getEmployee().getLastName() : "");

        return ManagerAssignmentResponse.builder()
                .id(dm.getId())
                .departmentId(dm.getDepartment().getId())
                .departmentName(dm.getDepartment().getName())
                .employeeId(dm.getEmployee().getId())
                .employeeCode(dm.getEmployee().getEmployeeCode())
                .employeeName(employeeName)
                .email(dm.getEmployee().getEmail())
                .isPrimary(dm.getIsPrimary())
                .createdAt(dm.getCreatedAt())
                .build();
    }
}
