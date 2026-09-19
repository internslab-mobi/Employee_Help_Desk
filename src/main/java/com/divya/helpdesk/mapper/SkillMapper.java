package com.divya.helpdesk.mapper;

import com.divya.helpdesk.dto.request.SkillCreateRequest;
import com.divya.helpdesk.dto.request.SkillUpdateRequest;
import com.divya.helpdesk.dto.response.AgentSkillResponse;
import com.divya.helpdesk.dto.response.SkillResponse;
import com.divya.helpdesk.dto.response.SubCategorySkillResponse;
import com.divya.helpdesk.entity.HDAgentSkill;
import com.divya.helpdesk.entity.HDSkill;
import com.divya.helpdesk.entity.HDSubCategorySkill;
import org.springframework.stereotype.Component;

@Component
public class SkillMapper {

    public SkillResponse toResponse(HDSkill entity) {
        if (entity == null) return null;
        SkillResponse response = new SkillResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setDescription(entity.getDescription());
        response.setIsActive(entity.getIsActive());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }

    public HDSkill toEntity(SkillCreateRequest request) {
        if (request == null) return null;
        HDSkill entity = new HDSkill();
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        return entity;
    }

    public void updateEntity(HDSkill entity, SkillUpdateRequest request) {
        if (entity == null || request == null) return;
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        if (request.getIsActive() != null) {
            entity.setIsActive(request.getIsActive());
        }
    }

    public AgentSkillResponse toAgentSkillResponse(HDAgentSkill entity) {
        if (entity == null) return null;
        AgentSkillResponse response = new AgentSkillResponse();
        response.setId(entity.getId());
        if (entity.getAgent() != null) {
            response.setAgentId(entity.getAgent().getId());
            if (entity.getAgent().getEmployee() != null) {
                response.setEmployeeId(entity.getAgent().getEmployee().getId());
                response.setEmployeeName(entity.getAgent().getEmployee().getFullName().trim());
            }
        }
        if (entity.getSkill() != null) {
            response.setSkillId(entity.getSkill().getId());
            response.setSkillName(entity.getSkill().getName());
        }
        response.setCreatedAt(entity.getCreatedAt());
        return response;
    }

    public SubCategorySkillResponse toSubCategorySkillResponse(HDSubCategorySkill entity) {
        if (entity == null) return null;
        SubCategorySkillResponse response = new SubCategorySkillResponse();
        response.setId(entity.getId());
        if (entity.getSubCategory() != null) {
            response.setSubCategoryId(entity.getSubCategory().getId());
            response.setSubCategoryName(entity.getSubCategory().getName());
        }
        if (entity.getSkill() != null) {
            response.setSkillId(entity.getSkill().getId());
            response.setSkillName(entity.getSkill().getName());
        }
        response.setCreatedAt(entity.getCreatedAt());
        return response;
    }
}
