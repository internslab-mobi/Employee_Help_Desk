package com.divya.helpdesk.service.impl;

import com.divya.helpdesk.dto.request.EmployeeCreateRequest;
import com.divya.helpdesk.dto.request.EmployeeUpdateRequest;
import com.divya.helpdesk.dto.response.EmployeeResponse;
import com.divya.helpdesk.entity.HDDepartment;
import com.divya.helpdesk.entity.HDEmployee;
import com.divya.helpdesk.exception.BadRequestException;
import com.divya.helpdesk.exception.ResourceNotFoundException;
import com.divya.helpdesk.mapper.EmployeeMapper;
import com.divya.helpdesk.repository.HDDepartmentRepository;
import com.divya.helpdesk.repository.HDEmployeeRepository;
import com.divya.helpdesk.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeServiceImpl implements EmployeeService {

    private final HDEmployeeRepository employeeRepository;
    private final HDDepartmentRepository departmentRepository;
    private final EmployeeMapper employeeMapper;

    @Override
    public EmployeeResponse createEmployee(EmployeeCreateRequest request) {
        if (employeeRepository.existsByEmployeeCode(request.getEmployeeCode())) {
            throw new BadRequestException("Employee with code '" + request.getEmployeeCode() + "' already exists");
        }
        if (employeeRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Employee with email '" + request.getEmail() + "' already exists");
        }

        HDEmployee employee = employeeMapper.toEntity(request);
        if (request.getDepartmentId() != null) {
            HDDepartment department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.getDepartmentId()));
            employee.setDepartment(department);
        }

        HDEmployee saved = employeeRepository.save(employee);
        return employeeMapper.toResponse(saved);
    }

    @Override
    public EmployeeResponse updateEmployee(Long id, EmployeeUpdateRequest request) {
        HDEmployee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));

        if (!employee.getEmail().equalsIgnoreCase(request.getEmail()) && employeeRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Employee with email '" + request.getEmail() + "' already exists");
        }

        employeeMapper.updateEntity(employee, request);
        if (request.getDepartmentId() != null) {
            HDDepartment department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.getDepartmentId()));
            employee.setDepartment(department);
        } else {
            employee.setDepartment(null);
        }

        HDEmployee saved = employeeRepository.save(employee);
        return employeeMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeById(Long id) {
        HDEmployee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        return employeeMapper.toResponse(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeResponse> getAllEmployees(Long departmentId) {
        List<HDEmployee> employees;
        if (departmentId != null) {
            employees = employeeRepository.findByDepartmentId(departmentId);
        } else {
            employees = employeeRepository.findAll();
        }
        return employees.stream()
                .map(employeeMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void uploadProfileImage(Long employeeId, MultipartFile file) {
        HDEmployee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty or missing");
        }

        try {
            employee.setProfileImage(file.getBytes());
            employee.setProfileImageType(file.getContentType());
            employeeRepository.save(employee);
        } catch (IOException e) {
            throw new BadRequestException("Failed to read image file: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getProfileImage(Long employeeId) {
        HDEmployee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));

        if (employee.getProfileImage() == null || employee.getProfileImage().length == 0) {
            throw new ResourceNotFoundException("Profile image not found for employee id: " + employeeId);
        }
        return employee.getProfileImage();
    }

    @Override
    @Transactional(readOnly = true)
    public String getProfileImageType(Long employeeId) {
        HDEmployee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));
        return employee.getProfileImageType() != null ? employee.getProfileImageType() : "image/jpeg";
    }
}
