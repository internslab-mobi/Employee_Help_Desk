package com.divya.helpdesk.mapper;

import com.divya.helpdesk.dto.request.EmployeeCreateRequest;
import com.divya.helpdesk.dto.request.EmployeeUpdateRequest;
import com.divya.helpdesk.dto.response.EmployeeResponse;
import com.divya.helpdesk.entity.HDEmployee;
import org.springframework.stereotype.Component;

@Component
public class EmployeeMapper {

    public EmployeeResponse toResponse(HDEmployee entity) {
        if (entity == null) return null;
        EmployeeResponse response = new EmployeeResponse();
        response.setId(entity.getId());
        response.setEmployeeCode(entity.getEmployeeCode());
        response.setFirstName(entity.getFirstName());
        response.setLastName(entity.getLastName());
        response.setFullName(entity.getFullName().trim());
        response.setEmail(entity.getEmail());
        response.setPhone(entity.getPhone());
        response.setDesignation(entity.getDesignation());
        if (entity.getDepartment() != null) {
            response.setDepartmentId(entity.getDepartment().getId());
            response.setDepartmentName(entity.getDepartment().getName());
        }
        response.setEmploymentStatus(entity.getEmploymentStatus());
        response.setDateOfJoining(entity.getDateOfJoining());
        response.setDateOfExit(entity.getDateOfExit());
        response.setHasProfileImage(entity.getProfileImage() != null && entity.getProfileImage().length > 0);
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }

    public HDEmployee toEntity(EmployeeCreateRequest request) {
        if (request == null) return null;
        HDEmployee entity = new HDEmployee();
        entity.setEmployeeCode(request.getEmployeeCode());
        entity.setFirstName(request.getFirstName());
        entity.setLastName(request.getLastName());
        entity.setEmail(request.getEmail());
        entity.setPhone(request.getPhone());
        entity.setDesignation(request.getDesignation());
        entity.setEmploymentStatus(request.getEmploymentStatus());
        entity.setDateOfJoining(request.getDateOfJoining());
        entity.setDateOfExit(request.getDateOfExit());
        return entity;
    }

    public void updateEntity(HDEmployee entity, EmployeeUpdateRequest request) {
        if (entity == null || request == null) return;
        entity.setFirstName(request.getFirstName());
        entity.setLastName(request.getLastName());
        entity.setEmail(request.getEmail());
        entity.setPhone(request.getPhone());
        entity.setDesignation(request.getDesignation());
        if (request.getEmploymentStatus() != null) {
            entity.setEmploymentStatus(request.getEmploymentStatus());
        }
        entity.setDateOfJoining(request.getDateOfJoining());
        entity.setDateOfExit(request.getDateOfExit());
    }
}
