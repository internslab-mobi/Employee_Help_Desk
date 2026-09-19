package com.divya.helpdesk.mapper;

import com.divya.helpdesk.dto.request.SubCategoryCreateRequest;
import com.divya.helpdesk.dto.request.SubCategoryUpdateRequest;
import com.divya.helpdesk.dto.response.SubCategoryResponse;
import com.divya.helpdesk.entity.HDSubCategory;
import org.springframework.stereotype.Component;

@Component
public class SubCategoryMapper {

    public SubCategoryResponse toResponse(HDSubCategory entity) {
        if (entity == null) return null;
        SubCategoryResponse response = new SubCategoryResponse();
        response.setId(entity.getId());
        if (entity.getCategory() != null) {
            response.setCategoryId(entity.getCategory().getId());
            response.setCategoryName(entity.getCategory().getName());
            if (entity.getCategory().getDepartment() != null) {
                response.setDepartmentId(entity.getCategory().getDepartment().getId());
                response.setDepartmentName(entity.getCategory().getDepartment().getName());
            }
        }
        response.setName(entity.getName());
        response.setDescription(entity.getDescription());
        response.setPriority(entity.getPriority());
        response.setIsActive(entity.getIsActive());
        if (entity.getCreatedBy() != null) {
            response.setCreatedById(entity.getCreatedBy().getId());
            response.setCreatedByName(entity.getCreatedBy().getFullName().trim());
        }
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }

    public HDSubCategory toEntity(SubCategoryCreateRequest request) {
        if (request == null) return null;
        HDSubCategory entity = new HDSubCategory();
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        if (request.getPriority() != null) {
            entity.setPriority(request.getPriority());
        }
        entity.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        return entity;
    }

    public void updateEntity(HDSubCategory entity, SubCategoryUpdateRequest request) {
        if (entity == null || request == null) return;
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        if (request.getPriority() != null) {
            entity.setPriority(request.getPriority());
        }
        if (request.getIsActive() != null) {
            entity.setIsActive(request.getIsActive());
        }
    }
}
