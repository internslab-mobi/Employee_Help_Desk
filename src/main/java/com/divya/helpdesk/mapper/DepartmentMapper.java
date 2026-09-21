package com.divya.helpdesk.mapper;

import com.divya.helpdesk.dto.request.DepartmentCreateRequest;
import com.divya.helpdesk.dto.request.DepartmentUpdateRequest;
import com.divya.helpdesk.dto.response.DepartmentAgentResponse;
import com.divya.helpdesk.dto.response.DepartmentManagerResponse;
import com.divya.helpdesk.dto.response.DepartmentResponse;
import com.divya.helpdesk.entity.HDDepartment;
import com.divya.helpdesk.entity.HDDepartmentAgent;
import com.divya.helpdesk.entity.HDDepartmentManager;
import org.springframework.stereotype.Component;

@Component
public class DepartmentMapper {

    public DepartmentResponse toResponse(HDDepartment entity) {
        if (entity == null) return null;
        DepartmentResponse response = new DepartmentResponse();
        response.setId(entity.getId());
        response.setCode(entity.getCode());
        response.setName(entity.getName());
        response.setDescription(entity.getDescription());
        response.setIsActive(entity.getIsActive());
        if (entity.getBusinessCalendar() != null) {
            response.setBusinessCalendarId(entity.getBusinessCalendar().getId());
            response.setBusinessCalendarName(entity.getBusinessCalendar().getName());
        }
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }

    public HDDepartment toEntity(DepartmentCreateRequest request) {
        if (request == null) return null;
        HDDepartment entity = new HDDepartment();
        entity.setCode(request.getCode());
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        return entity;
    }

    public void updateEntity(HDDepartment entity, DepartmentUpdateRequest request) {
        if (entity == null || request == null) return;
        entity.setCode(request.getCode());
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        if (request.getIsActive() != null) {
            entity.setIsActive(request.getIsActive());
        }
    }

    public DepartmentAgentResponse toAgentResponse(HDDepartmentAgent entity) {
        if (entity == null) return null;
        DepartmentAgentResponse response = new DepartmentAgentResponse();
        response.setId(entity.getId());
        if (entity.getDepartment() != null) {
            response.setDepartmentId(entity.getDepartment().getId());
            response.setDepartmentName(entity.getDepartment().getName());
        }
        if (entity.getEmployee() != null) {
            response.setEmployeeId(entity.getEmployee().getId());
            response.setEmployeeCode(entity.getEmployee().getEmployeeCode());
            response.setEmployeeName(entity.getEmployee().getFullName().trim());
            response.setEmail(entity.getEmployee().getEmail());
        }
        response.setLastAssignedAt(entity.getLastAssignedAt());
        response.setCreatedAt(entity.getCreatedAt());
        return response;
    }

    public DepartmentManagerResponse toManagerResponse(HDDepartmentManager entity) {
        if (entity == null) return null;
        DepartmentManagerResponse response = new DepartmentManagerResponse();
        response.setId(entity.getId());
        if (entity.getDepartment() != null) {
            response.setDepartmentId(entity.getDepartment().getId());
            response.setDepartmentName(entity.getDepartment().getName());
        }
        if (entity.getEmployee() != null) {
            response.setEmployeeId(entity.getEmployee().getId());
            response.setEmployeeCode(entity.getEmployee().getEmployeeCode());
            response.setEmployeeName(entity.getEmployee().getFullName().trim());
            response.setEmail(entity.getEmployee().getEmail());
        }
        response.setIsPrimary(entity.getIsPrimary());
        response.setCreatedAt(entity.getCreatedAt());
        return response;
    }
}
