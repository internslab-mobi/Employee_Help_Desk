package com.divya.helpdesk.service;

import com.divya.helpdesk.dto.request.SlaPolicyRequest;
import com.divya.helpdesk.dto.response.SlaPolicyResponse;

import java.util.List;

public interface SlaPolicyService {
    SlaPolicyResponse createOrUpdatePolicy(SlaPolicyRequest request);
    List<SlaPolicyResponse> getAllPolicies(Long departmentId);
    SlaPolicyResponse getPolicyByDeptAndSubCategory(Long departmentId, Long subCategoryId);
    SlaPolicyResponse getPolicyById(Long id);
    void deletePolicy(Long id);
}
