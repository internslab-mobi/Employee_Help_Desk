package com.divya.helpdesk.mapper;

import com.divya.helpdesk.dto.request.SlaPolicyRequest;
import com.divya.helpdesk.dto.response.SlaInstanceResponse;
import com.divya.helpdesk.dto.response.SlaPolicyResponse;
import com.divya.helpdesk.entity.HDSlaInstance;
import com.divya.helpdesk.entity.HDSlaPolicy;
import org.springframework.stereotype.Component;

@Component
public class SlaPolicyMapper {

    public SlaPolicyResponse toResponse(HDSlaPolicy entity) {
        if (entity == null) return null;
        SlaPolicyResponse response = new SlaPolicyResponse();
        response.setId(entity.getId());
        if (entity.getDepartment() != null) {
            response.setDepartmentId(entity.getDepartment().getId());
            response.setDepartmentName(entity.getDepartment().getName());
        }
        if (entity.getSubCategory() != null) {
            response.setSubCategoryId(entity.getSubCategory().getId());
            response.setSubCategoryName(entity.getSubCategory().getName());
        }
        response.setDurationMinutes(entity.getDurationMinutes());
        response.setWarningMinutes(entity.getWarningMinutes());
        response.setIsActive(entity.getIsActive());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }

    public HDSlaPolicy toEntity(SlaPolicyRequest request) {
        if (request == null) return null;
        HDSlaPolicy entity = new HDSlaPolicy();
        entity.setDurationMinutes(request.getDurationMinutes());
        int warning = request.getWarningMinutes() != null ?
                request.getWarningMinutes() :
                (int) Math.round(request.getDurationMinutes() * 0.75);
        entity.setWarningMinutes(warning);
        entity.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        return entity;
    }

    public SlaInstanceResponse toInstanceResponse(HDSlaInstance entity) {
        if (entity == null) return null;
        SlaInstanceResponse response = new SlaInstanceResponse();
        response.setId(entity.getId());
        if (entity.getTicket() != null) {
            response.setTicketId(entity.getTicket().getId());
        }
        if (entity.getSlaPolicy() != null) {
            response.setSlaPolicyId(entity.getSlaPolicy().getId());
        }
        response.setCycleNumber(entity.getCycleNumber());
        response.setAllocatedMinutes(entity.getAllocatedMinutes());
        response.setSlaStartAt(entity.getSlaStartAt());
        response.setOriginalDeadlineAt(entity.getOriginalDeadlineAt());
        response.setCurrentDeadlineAt(entity.getCurrentDeadlineAt());
        response.setWarningAt(entity.getWarningAt());
        response.setStatus(entity.getStatus());
        response.setPausedAt(entity.getPausedAt());
        response.setBreachedAt(entity.getBreachedAt());
        response.setCreatedAt(entity.getCreatedAt());
        return response;
    }
}
