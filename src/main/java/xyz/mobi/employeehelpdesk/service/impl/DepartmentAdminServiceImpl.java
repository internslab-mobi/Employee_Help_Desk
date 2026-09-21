package xyz.mobi.employeehelpdesk.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.mobi.employeehelpdesk.dto.department.CreateDepartmentRequest;
import xyz.mobi.employeehelpdesk.dto.department.DepartmentResponse;
import xyz.mobi.employeehelpdesk.dto.department.UpdateDepartmentRequest;
import xyz.mobi.employeehelpdesk.entity.Department;
import xyz.mobi.employeehelpdesk.exception.BadRequestException;
import xyz.mobi.employeehelpdesk.exception.ResourceNotFoundException;
import xyz.mobi.employeehelpdesk.repository.CategoryRepository;
import xyz.mobi.employeehelpdesk.repository.DepartmentRepository;
import xyz.mobi.employeehelpdesk.service.DepartmentAdminService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepartmentAdminServiceImpl implements DepartmentAdminService {

    private final DepartmentRepository departmentRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public DepartmentResponse createDepartment(CreateDepartmentRequest request) {
        if (departmentRepository.existsByCode(request.code().trim())) {
            throw new BadRequestException("Department code already exists: " + request.code());
        }

        if (departmentRepository.existsByName(request.name().trim())) {
            throw new BadRequestException("Department name already exists: " + request.name());
        }

        Department department = new Department();
        department.setCode(request.code().trim().toUpperCase());
        department.setName(request.name().trim());
        department.setDescription(request.description() != null ? request.description().trim() : null);
        department.setIsActive(true);

        Department saved = departmentRepository.save(department);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAllDepartments(Boolean activeOnly) {
        List<Department> list = Boolean.TRUE.equals(activeOnly)
                ? departmentRepository.findByIsActiveTrue()
                : departmentRepository.findAll();

        return list.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentResponse getDepartmentById(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
        return toResponse(department);
    }

    @Override
    @Transactional
    public DepartmentResponse updateDepartment(Long id, UpdateDepartmentRequest request) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));

        if (departmentRepository.existsByNameAndIdNot(request.name().trim(), id)) {
            throw new BadRequestException("Department name already exists: " + request.name());
        }

        department.setName(request.name().trim());
        department.setDescription(request.description() != null ? request.description().trim() : null);
        department.setIsActive(request.isActive());

        Department updated = departmentRepository.save(department);
        return toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteDepartment(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));

        if (!categoryRepository.findByDepartmentId(id).isEmpty()) {
            throw new BadRequestException("Cannot delete department with existing categories. Deactivate the department instead.");
        }

        try {
            departmentRepository.delete(department);
        } catch (Exception ex) {
            throw new BadRequestException("Cannot delete department due to existing references. Deactivate the department instead.");
        }
    }

    private DepartmentResponse toResponse(Department department) {
        return DepartmentResponse.builder()
                .id(department.getId())
                .code(department.getCode())
                .name(department.getName())
                .description(department.getDescription())
                .isActive(department.getIsActive())
                .createdAt(department.getCreatedAt())
                .updatedAt(department.getUpdatedAt())
                .build();
    }
}
