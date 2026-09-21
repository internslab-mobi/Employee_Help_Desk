package xyz.mobi.employeehelpdesk.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.mobi.employeehelpdesk.dto.employee.CreateEmployeeRequest;
import xyz.mobi.employeehelpdesk.dto.employee.EmployeeAdminResponse;
import xyz.mobi.employeehelpdesk.dto.employee.ResetPasswordRequest;
import xyz.mobi.employeehelpdesk.dto.employee.UpdateEmployeeRequest;
import xyz.mobi.employeehelpdesk.entity.Department;
import xyz.mobi.employeehelpdesk.entity.DepartmentAgent;
import xyz.mobi.employeehelpdesk.entity.Employee;
import xyz.mobi.employeehelpdesk.entity.enums.EmploymentStatus;
import xyz.mobi.employeehelpdesk.entity.enums.TicketStatus;
import xyz.mobi.employeehelpdesk.entity.enums.UserRole;
import xyz.mobi.employeehelpdesk.exception.BadRequestException;
import xyz.mobi.employeehelpdesk.exception.ResourceNotFoundException;
import xyz.mobi.employeehelpdesk.repository.DepartmentAgentRepository;
import xyz.mobi.employeehelpdesk.repository.DepartmentManagerRepository;
import xyz.mobi.employeehelpdesk.repository.DepartmentRepository;
import xyz.mobi.employeehelpdesk.repository.EmployeeRepository;
import xyz.mobi.employeehelpdesk.repository.TicketRepository;
import xyz.mobi.employeehelpdesk.service.EmployeeAdminService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeAdminServiceImpl implements EmployeeAdminService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final DepartmentAgentRepository departmentAgentRepository;
    private final DepartmentManagerRepository departmentManagerRepository;
    private final TicketRepository ticketRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public EmployeeAdminResponse createEmployee(CreateEmployeeRequest request) {
        if (employeeRepository.existsByEmployeeCode(request.employeeCode())) {
            throw new BadRequestException("Employee code already exists: " + request.employeeCode());
        }

        if (employeeRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email already exists: " + request.email());
        }

        Department department = null;
        if (request.departmentId() != null) {
            department = departmentRepository.findById(request.departmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + request.departmentId()));
        }

        String rawPassword = (request.initialPassword() != null && !request.initialPassword().isBlank())
                ? request.initialPassword()
                : "Welcome@123";

        Employee employee = new Employee();
        employee.setEmployeeCode(request.employeeCode().trim());
        employee.setFirstName(request.firstName().trim());
        employee.setLastName(request.lastName() != null ? request.lastName().trim() : null);
        employee.setEmail(request.email().trim().toLowerCase());
        employee.setPhone(request.phone() != null ? request.phone().trim() : null);
        employee.setDesignation(request.designation() != null ? request.designation().trim() : null);
        employee.setDepartment(department);
        employee.setEmploymentStatus(request.employmentStatus());
        employee.setRole(request.role());
        employee.setEnabled(true);
        employee.setPasswordHash(passwordEncoder.encode(rawPassword));
        employee.setDateOfJoining(request.dateOfJoining());

        Employee saved = employeeRepository.save(employee);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeAdminResponse> getAllEmployees(Long departmentId, EmploymentStatus status, UserRole role, Pageable pageable) {
        Page<Employee> page;
        if (departmentId != null) {
            page = employeeRepository.findByDepartmentId(departmentId, pageable);
        } else if (status != null) {
            page = employeeRepository.findByEmploymentStatus(status, pageable);
        } else if (role != null) {
            page = employeeRepository.findByRole(role, pageable);
        } else {
            page = employeeRepository.findAll(pageable);
        }
        return page.map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeAdminResponse getEmployeeById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        return toResponse(employee);
    }

    @Override
    @Transactional
    public EmployeeAdminResponse updateEmployee(Long id, UpdateEmployeeRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));

        // If changing role away from AGENT, verify no active assigned tickets
        if (employee.getRole() == UserRole.AGENT && request.role() != UserRole.AGENT && request.role() != UserRole.ADMIN) {
            List<DepartmentAgent> agents = departmentAgentRepository.findByEmployeeId(id);
            for (DepartmentAgent agent : agents) {
                long activeTickets = ticketRepository.countActiveTicketsForAgent(
                        agent.getId(),
                        List.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS, TicketStatus.ON_HOLD, TicketStatus.REOPENED)
                );
                if (activeTickets > 0) {
                    throw new BadRequestException("Cannot change role: Agent has active assigned tickets");
                }
            }
        }

        Department department = null;
        if (request.departmentId() != null) {
            department = departmentRepository.findById(request.departmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + request.departmentId()));
        }

        employee.setFirstName(request.firstName().trim());
        employee.setLastName(request.lastName() != null ? request.lastName().trim() : null);
        employee.setPhone(request.phone() != null ? request.phone().trim() : null);
        employee.setDesignation(request.designation() != null ? request.designation().trim() : null);
        employee.setDepartment(department);
        employee.setEmploymentStatus(request.employmentStatus());
        employee.setRole(request.role());
        employee.setEnabled(request.enabled());
        employee.setDateOfJoining(request.dateOfJoining());
        employee.setDateOfExit(request.dateOfExit());

        Employee updated = employeeRepository.save(employee);
        return toResponse(updated);
    }

    @Override
    @Transactional
    public EmployeeAdminResponse updateEmployeeStatus(Long id, Boolean enabled, EmploymentStatus status) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));

        if (enabled != null) {
            employee.setEnabled(enabled);
        }
        if (status != null) {
            employee.setEmploymentStatus(status);
        }

        Employee updated = employeeRepository.save(employee);
        return toResponse(updated);
    }

    @Override
    @Transactional
    public void resetPassword(Long id, ResetPasswordRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));

        employee.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        employeeRepository.save(employee);
    }

    @Override
    @Transactional
    public void deleteEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));

        // Check if employee is assigned as agent or manager
        if (!departmentAgentRepository.findByEmployeeId(id).isEmpty()
                || !departmentManagerRepository.findByEmployeeId(id).isEmpty()) {
            throw new BadRequestException("Cannot delete employee: employee is assigned as agent or manager. Remove assignments first or deactivate the employee.");
        }

        try {
            employeeRepository.delete(employee);
        } catch (Exception ex) {
            throw new BadRequestException("Cannot delete employee due to existing transactional references. Deactivate the employee instead.");
        }
    }

    private EmployeeAdminResponse toResponse(Employee employee) {
        return EmployeeAdminResponse.builder()
                .id(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .email(employee.getEmail())
                .phone(employee.getPhone())
                .designation(employee.getDesignation())
                .departmentId(employee.getDepartment() != null ? employee.getDepartment().getId() : null)
                .departmentName(employee.getDepartment() != null ? employee.getDepartment().getName() : null)
                .employmentStatus(employee.getEmploymentStatus())
                .role(employee.getRole())
                .enabled(employee.getEnabled())
                .dateOfJoining(employee.getDateOfJoining())
                .dateOfExit(employee.getDateOfExit())
                .createdAt(employee.getCreatedAt())
                .updatedAt(employee.getUpdatedAt())
                .build();
    }
}
