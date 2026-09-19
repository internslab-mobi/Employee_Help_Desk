package com.divya.helpdesk.service.impl;

import com.divya.helpdesk.dto.request.SlaPolicyRequest;
import com.divya.helpdesk.dto.response.SlaPolicyResponse;
import com.divya.helpdesk.entity.HDDepartment;
import com.divya.helpdesk.entity.HDSlaPolicy;
import com.divya.helpdesk.entity.HDSubCategory;
import com.divya.helpdesk.exception.ResourceNotFoundException;
import com.divya.helpdesk.mapper.SlaPolicyMapper;
import com.divya.helpdesk.repository.HDDepartmentRepository;
import com.divya.helpdesk.repository.HDSlaPolicyRepository;
import com.divya.helpdesk.repository.HDSubCategoryRepository;
import com.divya.helpdesk.service.SlaPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SlaPolicyServiceImpl implements SlaPolicyService {

    private final HDSlaPolicyRepository slaPolicyRepository;
    private final HDDepartmentRepository departmentRepository;
    private final HDSubCategoryRepository subCategoryRepository;
    private final SlaPolicyMapper slaPolicyMapper;

    @Override
    public SlaPolicyResponse createOrUpdatePolicy(SlaPolicyRequest request) {
        HDDepartment department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.getDepartmentId()));

        HDSubCategory subCategory = subCategoryRepository.findById(request.getSubCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("SubCategory not found with id: " + request.getSubCategoryId()));

        Optional<HDSlaPolicy> existingOpt = slaPolicyRepository.findByDepartmentIdAndSubCategoryId(
                request.getDepartmentId(), request.getSubCategoryId());

        HDSlaPolicy policy;
        if (existingOpt.isPresent()) {
            policy = existingOpt.get();
            policy.setDurationMinutes(request.getDurationMinutes());
            int warning = request.getWarningMinutes() != null ?
                    request.getWarningMinutes() :
                    (int) Math.round(request.getDurationMinutes() * 0.75);
            policy.setWarningMinutes(warning);
            if (request.getIsActive() != null) {
                policy.setIsActive(request.getIsActive());
            }
        } else {
            policy = slaPolicyMapper.toEntity(request);
            policy.setDepartment(department);
            policy.setSubCategory(subCategory);
        }

        HDSlaPolicy saved = slaPolicyRepository.save(policy);
        return slaPolicyMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SlaPolicyResponse> getAllPolicies(Long departmentId) {
        List<HDSlaPolicy> policies;
        if (departmentId != null) {
            policies = slaPolicyRepository.findByDepartmentId(departmentId);
        } else {
            policies = slaPolicyRepository.findAll();
        }
        return policies.stream()
                .map(slaPolicyMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SlaPolicyResponse getPolicyByDeptAndSubCategory(Long departmentId, Long subCategoryId) {
        HDSlaPolicy policy = slaPolicyRepository.findByDepartmentIdAndSubCategoryId(departmentId, subCategoryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "SLA Policy not found for department id " + departmentId + " and subcategory id " + subCategoryId));
        return slaPolicyMapper.toResponse(policy);
    }

    @Override
    @Transactional(readOnly = true)
    public SlaPolicyResponse getPolicyById(Long id) {
        HDSlaPolicy policy = slaPolicyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SLA Policy not found with id: " + id));
        return slaPolicyMapper.toResponse(policy);
    }

    @Override
    public void deletePolicy(Long id) {
        if (!slaPolicyRepository.existsById(id)) {
            throw new ResourceNotFoundException("SLA Policy not found with id: " + id);
        }
        slaPolicyRepository.deleteById(id);
    }
}
