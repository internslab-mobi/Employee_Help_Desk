package com.divya.helpdesk.service.impl;

import com.divya.helpdesk.dto.request.*;
import com.divya.helpdesk.dto.response.DepartmentAgentResponse;
import com.divya.helpdesk.dto.response.DepartmentManagerResponse;
import com.divya.helpdesk.dto.response.DepartmentResponse;
import com.divya.helpdesk.entity.HDBusinessCalendar;
import com.divya.helpdesk.entity.HDDepartment;
import com.divya.helpdesk.entity.HDDepartmentAgent;
import com.divya.helpdesk.entity.HDDepartmentManager;
import com.divya.helpdesk.entity.HDEmployee;
import com.divya.helpdesk.exception.BadRequestException;
import com.divya.helpdesk.exception.ResourceNotFoundException;
import com.divya.helpdesk.mapper.DepartmentMapper;
import com.divya.helpdesk.repository.*;
import com.divya.helpdesk.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class DepartmentServiceImpl implements DepartmentService {

    private final HDDepartmentRepository departmentRepository;
    private final HDDepartmentAgentRepository departmentAgentRepository;
    private final HDDepartmentManagerRepository departmentManagerRepository;
    private final HDEmployeeRepository employeeRepository;
    private final HDBusinessCalendarRepository calendarRepository;
    private final DepartmentMapper departmentMapper;

    @Override
    public DepartmentResponse createDepartment(DepartmentCreateRequest request) {
        if (departmentRepository.existsByCode(request.getCode())) {
            throw new BadRequestException("Department with code '" + request.getCode() + "' already exists");
        }

        HDDepartment department = departmentMapper.toEntity(request);
        if (request.getBusinessCalendarId() != null) {
            HDBusinessCalendar calendar = calendarRepository.findById(request.getBusinessCalendarId())
                    .orElseThrow(() -> new ResourceNotFoundException("Calendar not found with id: " + request.getBusinessCalendarId()));
            department.setBusinessCalendar(calendar);
        }

        HDDepartment saved = departmentRepository.save(department);
        return departmentMapper.toResponse(saved);
    }

    @Override
    public DepartmentResponse updateDepartment(Long id, DepartmentUpdateRequest request) {
        HDDepartment department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));

        if (!department.getCode().equals(request.getCode()) && departmentRepository.existsByCode(request.getCode())) {
            throw new BadRequestException("Department with code '" + request.getCode() + "' already exists");
        }

        departmentMapper.updateEntity(department, request);
        if (request.getBusinessCalendarId() != null) {
            HDBusinessCalendar calendar = calendarRepository.findById(request.getBusinessCalendarId())
                    .orElseThrow(() -> new ResourceNotFoundException("Calendar not found with id: " + request.getBusinessCalendarId()));
            department.setBusinessCalendar(calendar);
        } else {
            department.setBusinessCalendar(null);
        }

        HDDepartment saved = departmentRepository.save(department);
        return departmentMapper.toResponse(saved);
    }

    @Override
    public DepartmentResponse patchDepartment(Long id, DepartmentPatchRequest request) {
        HDDepartment department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));

        if (request.getCode() != null) {
            if (!department.getCode().equals(request.getCode()) && departmentRepository.existsByCode(request.getCode())) {
                throw new BadRequestException("Department with code '" + request.getCode() + "' already exists");
            }
            department.setCode(request.getCode());
        }
        if (request.getName() != null) {
            department.setName(request.getName());
        }
        if (request.getDescription() != null) {
            department.setDescription(request.getDescription());
        }
        if (request.getIsActive() != null) {
            department.setIsActive(request.getIsActive());
        }
        if (request.getBusinessCalendarId() != null) {
            HDBusinessCalendar calendar = calendarRepository.findById(request.getBusinessCalendarId())
                    .orElseThrow(() -> new ResourceNotFoundException("Calendar not found with id: " + request.getBusinessCalendarId()));
            department.setBusinessCalendar(calendar);
        }

        HDDepartment saved = departmentRepository.save(department);
        return departmentMapper.toResponse(saved);
    }

    @Override
    public void deleteDepartment(Long id) {
        if (!departmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Department not found with id: " + id);
        }
        departmentRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentResponse getDepartmentById(Long id) {
        HDDepartment department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
        return departmentMapper.toResponse(department);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(departmentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public DepartmentAgentResponse addAgent(Long departmentId, DepartmentAgentRequest request) {
        HDDepartment department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + departmentId));

        HDEmployee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + request.getEmployeeId()));

        if (departmentAgentRepository.existsByDepartmentIdAndEmployeeId(departmentId, request.getEmployeeId())) {
            throw new BadRequestException("Employee is already assigned as an agent in this department");
        }

        HDDepartmentAgent agent = new HDDepartmentAgent();
        agent.setDepartment(department);
        agent.setEmployee(employee);

        HDDepartmentAgent saved = departmentAgentRepository.save(agent);
        return departmentMapper.toAgentResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentAgentResponse> getAgentsByDepartment(Long departmentId) {
        if (!departmentRepository.existsById(departmentId)) {
            throw new ResourceNotFoundException("Department not found with id: " + departmentId);
        }
        return departmentAgentRepository.findByDepartmentId(departmentId).stream()
                .map(departmentMapper::toAgentResponse)
                .collect(Collectors.toList());
    }

    @Override
    public DepartmentManagerResponse addManager(Long departmentId, DepartmentManagerRequest request) {
        HDDepartment department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + departmentId));

        HDEmployee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + request.getEmployeeId()));

        if (departmentManagerRepository.existsByDepartmentIdAndEmployeeId(departmentId, request.getEmployeeId())) {
            throw new BadRequestException("Employee is already assigned as a manager in this department");
        }

        if (Boolean.TRUE.equals(request.getIsPrimary())) {
            // Unset previous primary manager
            departmentManagerRepository.findByDepartmentId(departmentId).forEach(dm -> {
                if (Boolean.TRUE.equals(dm.getIsPrimary())) {
                    dm.setIsPrimary(false);
                    departmentManagerRepository.save(dm);
                }
            });
        }

        HDDepartmentManager manager = new HDDepartmentManager();
        manager.setDepartment(department);
        manager.setEmployee(employee);
        manager.setIsPrimary(request.getIsPrimary() != null ? request.getIsPrimary() : false);

        HDDepartmentManager saved = departmentManagerRepository.save(manager);
        return departmentMapper.toManagerResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentManagerResponse> getManagersByDepartment(Long departmentId) {
        if (!departmentRepository.existsById(departmentId)) {
            throw new ResourceNotFoundException("Department not found with id: " + departmentId);
        }
        return departmentManagerRepository.findByDepartmentId(departmentId).stream()
                .map(departmentMapper::toManagerResponse)
                .collect(Collectors.toList());
    }
}
