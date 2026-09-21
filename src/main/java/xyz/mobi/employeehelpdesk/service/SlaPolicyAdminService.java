package xyz.mobi.employeehelpdesk.service;

import xyz.mobi.employeehelpdesk.dto.slapolicy.CreateSlaPolicyRequest;
import xyz.mobi.employeehelpdesk.dto.slapolicy.SlaPolicyResponse;
import xyz.mobi.employeehelpdesk.dto.slapolicy.UpdateSlaPolicyRequest;

import java.util.List;

public interface SlaPolicyAdminService {

    SlaPolicyResponse createSlaPolicy(CreateSlaPolicyRequest request);

    List<SlaPolicyResponse> getAllSlaPolicies(Long departmentId, Long subCategoryId, Boolean activeOnly);

    SlaPolicyResponse getSlaPolicyById(Long id);

    SlaPolicyResponse updateSlaPolicy(Long id, UpdateSlaPolicyRequest request);

    void deleteSlaPolicy(Long id);
}
