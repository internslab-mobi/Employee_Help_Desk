package com.divya.helpdesk.service;

import com.divya.helpdesk.dto.request.*;
import com.divya.helpdesk.dto.response.DepartmentAgentResponse;
import com.divya.helpdesk.dto.response.DepartmentManagerResponse;
import com.divya.helpdesk.dto.response.DepartmentResponse;

import java.util.List;

public interface DepartmentService {
    DepartmentResponse createDepartment(DepartmentCreateRequest request);
    DepartmentResponse updateDepartment(Long id, DepartmentUpdateRequest request);
    DepartmentResponse patchDepartment(Long id, DepartmentPatchRequest request);
    void deleteDepartment(Long id);
    DepartmentResponse getDepartmentById(Long id);
    List<DepartmentResponse> getAllDepartments();

    DepartmentAgentResponse addAgent(Long departmentId, DepartmentAgentRequest request);
    List<DepartmentAgentResponse> getAgentsByDepartment(Long departmentId);

    DepartmentManagerResponse addManager(Long departmentId, DepartmentManagerRequest request);
    List<DepartmentManagerResponse> getManagersByDepartment(Long departmentId);
}
