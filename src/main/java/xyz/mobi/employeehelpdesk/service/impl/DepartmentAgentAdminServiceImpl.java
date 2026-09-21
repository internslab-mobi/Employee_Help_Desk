package xyz.mobi.employeehelpdesk.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.mobi.employeehelpdesk.dto.agent.AgentAssignmentResponse;
import xyz.mobi.employeehelpdesk.dto.agent.AssignAgentRequest;
import xyz.mobi.employeehelpdesk.entity.Department;
import xyz.mobi.employeehelpdesk.entity.DepartmentAgent;
import xyz.mobi.employeehelpdesk.entity.Employee;
import xyz.mobi.employeehelpdesk.entity.enums.EmploymentStatus;
import xyz.mobi.employeehelpdesk.entity.enums.TicketStatus;
import xyz.mobi.employeehelpdesk.entity.enums.UserRole;
import xyz.mobi.employeehelpdesk.exception.BadRequestException;
import xyz.mobi.employeehelpdesk.exception.ResourceNotFoundException;
import xyz.mobi.employeehelpdesk.repository.AgentSkillRepository;
import xyz.mobi.employeehelpdesk.repository.DepartmentAgentRepository;
import xyz.mobi.employeehelpdesk.repository.DepartmentRepository;
import xyz.mobi.employeehelpdesk.repository.EmployeeRepository;
import xyz.mobi.employeehelpdesk.repository.TicketRepository;
import xyz.mobi.employeehelpdesk.service.CurrentUserService;
import xyz.mobi.employeehelpdesk.service.DepartmentAgentAdminService;
import xyz.mobi.employeehelpdesk.service.DepartmentAuthorizationService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepartmentAgentAdminServiceImpl implements DepartmentAgentAdminService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentAgentRepository departmentAgentRepository;
    private final AgentSkillRepository agentSkillRepository;
    private final TicketRepository ticketRepository;
    private final CurrentUserService currentUserService;
    private final DepartmentAuthorizationService departmentAuthorizationService;

    @Override
    @Transactional
    public AgentAssignmentResponse assignAgent(Long departmentId, AssignAgentRequest request) {
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

        if (employee.getRole() != UserRole.AGENT) {
            throw new BadRequestException("Employee does not have ROLE_AGENT (role: " + employee.getRole() + ")");
        }

        if (departmentAgentRepository.existsByEmployeeIdAndDepartmentId(request.employeeId(), departmentId)) {
            throw new BadRequestException("Employee is already assigned to this department as an agent");
        }

        DepartmentAgent departmentAgent = new DepartmentAgent();
        departmentAgent.setDepartment(department);
        departmentAgent.setEmployee(employee);

        DepartmentAgent saved = departmentAgentRepository.save(departmentAgent);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgentAssignmentResponse> getDepartmentAgents(Long departmentId) {
        checkDepartmentAccess(departmentId);

        if (!departmentRepository.existsById(departmentId)) {
            throw new ResourceNotFoundException("Department not found with id: " + departmentId);
        }

        return departmentAgentRepository.findByDepartmentId(departmentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgentAssignmentResponse> getAgentDepartments(Long employeeId) {
        checkAdminOrManagerRole();

        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employee not found with id: " + employeeId);
        }

        return departmentAgentRepository.findByEmployeeId(employeeId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void unassignAgent(Long departmentId, Long employeeId) {
        checkDepartmentAccess(departmentId);

        if (!departmentRepository.existsById(departmentId)) {
            throw new ResourceNotFoundException("Department not found with id: " + departmentId);
        }

        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employee not found with id: " + employeeId);
        }

        DepartmentAgent deptAgent = departmentAgentRepository.findByDepartmentIdAndEmployeeId(departmentId, employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent assignment not found for department " + departmentId + " and employee " + employeeId));

        long activeTickets = ticketRepository.countActiveTicketsForAgent(
                deptAgent.getId(),
                List.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS, TicketStatus.ON_HOLD, TicketStatus.REOPENED)
        );

        if (activeTickets > 0) {
            throw new BadRequestException("Cannot unassign agent with active tickets (" + activeTickets + " active ticket(s) found). Please reassign tickets first.");
        }

        agentSkillRepository.deleteByAgentId(deptAgent.getId());
        departmentAgentRepository.delete(deptAgent);
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

    private AgentAssignmentResponse toResponse(DepartmentAgent da) {
        String employeeName = da.getEmployee().getFirstName()
                + (da.getEmployee().getLastName() != null ? " " + da.getEmployee().getLastName() : "");

        return AgentAssignmentResponse.builder()
                .id(da.getId())
                .departmentId(da.getDepartment().getId())
                .departmentName(da.getDepartment().getName())
                .employeeId(da.getEmployee().getId())
                .employeeCode(da.getEmployee().getEmployeeCode())
                .employeeName(employeeName)
                .email(da.getEmployee().getEmail())
                .lastAssignedAt(da.getLastAssignedAt())
                .createdAt(da.getCreatedAt())
                .build();
    }
}
