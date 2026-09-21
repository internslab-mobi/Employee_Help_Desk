package xyz.mobi.employeehelpdesk.service;

import xyz.mobi.employeehelpdesk.dto.manager.AssignManagerRequest;
import xyz.mobi.employeehelpdesk.dto.manager.ManagerAssignmentResponse;

import java.util.List;

public interface DepartmentManagerAdminService {

    ManagerAssignmentResponse assignManager(Long departmentId, AssignManagerRequest request);

    List<ManagerAssignmentResponse> getDepartmentManagers(Long departmentId);

    List<ManagerAssignmentResponse> getManagerDepartments(Long employeeId);

    void unassignManager(Long departmentId, Long employeeId);
}
