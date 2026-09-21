package xyz.mobi.employeehelpdesk.service;

public interface DepartmentAuthorizationService {

    boolean isManagerOfDepartment(Long employeeId, Long departmentId);

    boolean isAgentOfDepartment(Long employeeId, Long departmentId);
}
