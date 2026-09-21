package xyz.mobi.employeehelpdesk.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.mobi.employeehelpdesk.dto.slapolicy.CreateSlaPolicyRequest;
import xyz.mobi.employeehelpdesk.dto.slapolicy.SlaPolicyResponse;
import xyz.mobi.employeehelpdesk.dto.slapolicy.UpdateSlaPolicyRequest;
import xyz.mobi.employeehelpdesk.entity.Department;
import xyz.mobi.employeehelpdesk.entity.SlaPolicy;
import xyz.mobi.employeehelpdesk.entity.SubCategory;
import xyz.mobi.employeehelpdesk.entity.enums.UserRole;
import xyz.mobi.employeehelpdesk.exception.BadRequestException;
import xyz.mobi.employeehelpdesk.exception.ResourceNotFoundException;
import xyz.mobi.employeehelpdesk.repository.DepartmentRepository;
import xyz.mobi.employeehelpdesk.repository.SlaInstanceRepository;
import xyz.mobi.employeehelpdesk.repository.SlaPolicyRepository;
import xyz.mobi.employeehelpdesk.repository.SubCategoryRepository;
import xyz.mobi.employeehelpdesk.service.CurrentUserService;
import xyz.mobi.employeehelpdesk.service.DepartmentAuthorizationService;
import xyz.mobi.employeehelpdesk.service.SlaPolicyAdminService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlaPolicyAdminServiceImpl implements SlaPolicyAdminService {

    private final SlaPolicyRepository slaPolicyRepository;
    private final DepartmentRepository departmentRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final SlaInstanceRepository slaInstanceRepository;
    private final CurrentUserService currentUserService;
    private final DepartmentAuthorizationService departmentAuthorizationService;

    @Override
    @Transactional
    public SlaPolicyResponse createSlaPolicy(CreateSlaPolicyRequest request) {
        checkDepartmentAccess(request.departmentId());

        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.departmentId()));

        if (!Boolean.TRUE.equals(department.getIsActive())) {
            throw new BadRequestException("Department is inactive: " + department.getName());
        }

        SubCategory subCategory = subCategoryRepository.findById(request.subCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("SubCategory not found with id: " + request.subCategoryId()));

        if (!Boolean.TRUE.equals(subCategory.getIsActive())) {
            throw new BadRequestException("SubCategory is inactive: " + subCategory.getName());
        }

        if (!subCategory.getCategory().getDepartment().getId().equals(request.departmentId())) {
            throw new BadRequestException("SubCategory does not belong to the specified department");
        }

        validateTiming(request.durationMinutes(), request.warningMinutes());

        if (slaPolicyRepository.existsByDepartmentIdAndSubCategoryId(request.departmentId(), request.subCategoryId())) {
            throw new BadRequestException("An SLA policy already exists for this department and subcategory");
        }

        SlaPolicy policy = new SlaPolicy();
        policy.setDepartment(department);
        policy.setSubCategory(subCategory);
        policy.setDurationMinutes(request.durationMinutes());
        policy.setWarningMinutes(request.warningMinutes());
        policy.setIsActive(true);

        SlaPolicy saved = slaPolicyRepository.save(policy);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SlaPolicyResponse> getAllSlaPolicies(Long departmentId, Long subCategoryId, Boolean activeOnly) {
        List<SlaPolicy> list;

        if (departmentId != null && subCategoryId != null) {
            list = slaPolicyRepository.findByDepartmentIdAndSubCategoryId(departmentId, subCategoryId);
            if (Boolean.TRUE.equals(activeOnly)) {
                list = list.stream().filter(p -> Boolean.TRUE.equals(p.getIsActive())).toList();
            }
        } else if (departmentId != null) {
            list = Boolean.TRUE.equals(activeOnly)
                    ? slaPolicyRepository.findByDepartmentIdAndIsActiveTrue(departmentId)
                    : slaPolicyRepository.findByDepartmentId(departmentId);
        } else if (subCategoryId != null) {
            list = Boolean.TRUE.equals(activeOnly)
                    ? slaPolicyRepository.findBySubCategoryIdAndIsActiveTrue(subCategoryId)
                    : slaPolicyRepository.findBySubCategoryId(subCategoryId);
        } else {
            list = Boolean.TRUE.equals(activeOnly)
                    ? slaPolicyRepository.findByIsActiveTrue()
                    : slaPolicyRepository.findAll();
        }

        return list.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SlaPolicyResponse getSlaPolicyById(Long id) {
        SlaPolicy policy = slaPolicyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SlaPolicy not found with id: " + id));
        return toResponse(policy);
    }

    @Override
    @Transactional
    public SlaPolicyResponse updateSlaPolicy(Long id, UpdateSlaPolicyRequest request) {
        SlaPolicy policy = slaPolicyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SlaPolicy not found with id: " + id));

        checkDepartmentAccess(policy.getDepartment().getId());
        validateTiming(request.durationMinutes(), request.warningMinutes());

        policy.setDurationMinutes(request.durationMinutes());
        policy.setWarningMinutes(request.warningMinutes());
        policy.setIsActive(request.isActive());

        SlaPolicy updated = slaPolicyRepository.save(policy);
        return toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteSlaPolicy(Long id) {
        SlaPolicy policy = slaPolicyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SlaPolicy not found with id: " + id));

        checkDepartmentAccess(policy.getDepartment().getId());

        if (slaInstanceRepository.existsBySlaPolicyId(id)) {
            throw new BadRequestException("Cannot delete SLA policy referenced by existing SLA instances. Deactivate the policy instead.");
        }

        try {
            slaPolicyRepository.delete(policy);
        } catch (Exception ex) {
            throw new BadRequestException("Cannot delete SLA policy due to existing references. Deactivate the policy instead.");
        }
    }

    private void validateTiming(Integer durationMinutes, Integer warningMinutes) {
        if (durationMinutes == null || durationMinutes <= 0) {
            throw new BadRequestException("Duration must be greater than 0");
        }
        if (warningMinutes != null) {
            if (warningMinutes <= 0) {
                throw new BadRequestException("Warning duration must be greater than 0");
            }
            if (warningMinutes >= durationMinutes) {
                throw new BadRequestException("Warning duration (" + warningMinutes + ") must be less than duration (" + durationMinutes + ")");
            }
        }
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

    private SlaPolicyResponse toResponse(SlaPolicy policy) {
        return SlaPolicyResponse.builder()
                .id(policy.getId())
                .departmentId(policy.getDepartment().getId())
                .departmentName(policy.getDepartment().getName())
                .subCategoryId(policy.getSubCategory().getId())
                .subCategoryName(policy.getSubCategory().getName())
                .durationMinutes(policy.getDurationMinutes())
                .warningMinutes(policy.getWarningMinutes())
                .isActive(policy.getIsActive())
                .createdAt(policy.getCreatedAt())
                .updatedAt(policy.getUpdatedAt())
                .build();
    }
}
