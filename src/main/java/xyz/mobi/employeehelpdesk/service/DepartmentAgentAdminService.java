package xyz.mobi.employeehelpdesk.service;

import xyz.mobi.employeehelpdesk.dto.agent.AgentAssignmentResponse;
import xyz.mobi.employeehelpdesk.dto.agent.AssignAgentRequest;

import java.util.List;

public interface DepartmentAgentAdminService {

    AgentAssignmentResponse assignAgent(Long departmentId, AssignAgentRequest request);

    List<AgentAssignmentResponse> getDepartmentAgents(Long departmentId);

    List<AgentAssignmentResponse> getAgentDepartments(Long employeeId);

    void unassignAgent(Long departmentId, Long employeeId);
}
