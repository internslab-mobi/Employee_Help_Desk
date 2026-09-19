package com.divya.helpdesk.mapper;

import com.divya.helpdesk.dto.response.TicketDetailsResponse;
import com.divya.helpdesk.dto.response.TicketResponse;
import com.divya.helpdesk.entity.HDSlaInstance;
import com.divya.helpdesk.entity.HDTicket;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TicketMapper {

    private final EmployeeMapper employeeMapper;
    private final DepartmentMapper departmentMapper;
    private final CategoryMapper categoryMapper;
    private final SubCategoryMapper subCategoryMapper;
    private final SlaPolicyMapper slaPolicyMapper;


    public TicketResponse toResponse(HDTicket entity, HDSlaInstance slaInstance) {
        if (entity == null) return null;
        TicketResponse response = new TicketResponse();
        response.setId(entity.getId());
        response.setTicketNumber(entity.getTicketNumber());
        if (entity.getRequester() != null) {
            response.setRequesterId(entity.getRequester().getId());
            response.setRequesterName(entity.getRequester().getFullName().trim());
        }
        if (entity.getDepartment() != null) {
            response.setDepartmentId(entity.getDepartment().getId());
            response.setDepartmentName(entity.getDepartment().getName());
        }
        if (entity.getCategory() != null) {
            response.setCategoryId(entity.getCategory().getId());
            response.setCategoryName(entity.getCategory().getName());
        }
        if (entity.getSubCategory() != null) {
            response.setSubCategoryId(entity.getSubCategory().getId());
            response.setSubCategoryName(entity.getSubCategory().getName());
        }
        response.setSubject(entity.getSubject());
        response.setDescription(entity.getDescription());
        response.setStatus(entity.getStatus());
        if (entity.getAssignedAgent() != null) {
            response.setAssignedAgentId(entity.getAssignedAgent().getId());
            response.setAssignedAgentName(entity.getAssignedAgent().getFullName().trim());
        }
        if (entity.getAssignedManager() != null) {
            response.setAssignedManagerId(entity.getAssignedManager().getId());
            response.setAssignedManagerName(entity.getAssignedManager().getFullName().trim());
        }
        response.setPriority(entity.getPriority());
        response.setReopenCount(entity.getReopenCount());
        response.setResolutionSummary(entity.getResolutionSummary());
        response.setResolvedAt(entity.getResolvedAt());
        response.setClosedAt(entity.getClosedAt());
        response.setWithdrawnAt(entity.getWithdrawnAt());

        if (slaInstance != null) {
            response.setDueAt(slaInstance.getCurrentDeadlineAt());
        }

        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }

    public TicketDetailsResponse toDetailsResponse(HDTicket entity, HDSlaInstance slaInstance) {
        if (entity == null) return null;
        TicketDetailsResponse response = new TicketDetailsResponse();
        response.setId(entity.getId());
        response.setTicketNumber(entity.getTicketNumber());
        response.setRequester(employeeMapper.toResponse(entity.getRequester()));
        response.setDepartment(departmentMapper.toResponse(entity.getDepartment()));
        response.setCategory(categoryMapper.toResponse(entity.getCategory()));
        response.setSubCategory(subCategoryMapper.toResponse(entity.getSubCategory()));
        response.setSubject(entity.getSubject());
        response.setDescription(entity.getDescription());
        response.setStatus(entity.getStatus());
        response.setAssignedAgent(employeeMapper.toResponse(entity.getAssignedAgent()));
        response.setAssignedManager(employeeMapper.toResponse(entity.getAssignedManager()));
        response.setSlaPolicy(slaPolicyMapper.toResponse(entity.getSlaPolicy()));
        if (slaInstance != null) {
            response.setSlaInstance(slaPolicyMapper.toInstanceResponse(slaInstance));
            response.setDueAt(slaInstance.getCurrentDeadlineAt());
        }
        response.setPriority(entity.getPriority());
        response.setReopenCount(entity.getReopenCount());
        response.setResolutionSummary(entity.getResolutionSummary());
        response.setHoldReason(entity.getHoldReason());
        response.setHoldStartedAt(entity.getHoldStartedAt());
        response.setWithdrawalReason(entity.getWithdrawalReason());
        response.setResolvedAt(entity.getResolvedAt());
        response.setClosedAt(entity.getClosedAt());
        response.setWithdrawnAt(entity.getWithdrawnAt());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
}
