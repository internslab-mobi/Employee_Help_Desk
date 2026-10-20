package xyz.mobi.employeehelpdesk.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.mobi.employeehelpdesk.dto.department.DepartmentResponseDTO;
import xyz.mobi.employeehelpdesk.entity.Department;
import xyz.mobi.employeehelpdesk.exception.ResourceNotFoundException;
import xyz.mobi.employeehelpdesk.repository.DepartmentRepository;
import xyz.mobi.employeehelpdesk.service.DepartmentService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;

    @Override
    public List<DepartmentResponseDTO> getAllDepartments() {
        log.debug("Fetching all active departments");
        return departmentRepository.findByIsActiveTrueOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public DepartmentResponseDTO getDepartmentById(Long id) {
        log.debug("Fetching department by id: {}", id);
        return departmentRepository.findByIdAndIsActiveTrue(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
    }

    @Override
    @Transactional
    public DepartmentResponseDTO createDepartment(xyz.mobi.employeehelpdesk.dto.department.CreateDepartmentRequestDTO request) {
        log.info("Creating new department: code={}, name={}", request.code(), request.name());
        Department department = new Department();
        department.setCode(request.code().trim().toUpperCase());
        department.setName(request.name().trim());
        department.setDescription(request.description() != null ? request.description().trim() : null);
        department.setTimezone(request.timezone() != null && !request.timezone().isBlank() ? request.timezone().trim() : "UTC");
        department.setIsActive(true);

        Department saved = departmentRepository.save(department);
        return toResponse(saved);
    }

    private DepartmentResponseDTO toResponse(Department department) {
        return DepartmentResponseDTO.builder()
                .id(department.getId())
                .code(department.getCode())
                .name(department.getName())
                .description(department.getDescription())
                .isActive(department.getIsActive())
                .timezone(department.getTimezone())
                .build();
    }
}
