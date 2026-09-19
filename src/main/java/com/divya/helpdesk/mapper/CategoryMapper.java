package com.divya.helpdesk.mapper;

import com.divya.helpdesk.dto.request.CategoryCreateRequest;
import com.divya.helpdesk.dto.request.CategoryUpdateRequest;
import com.divya.helpdesk.dto.response.CategoryResponse;
import com.divya.helpdesk.entity.HDCategory;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public CategoryResponse toResponse(HDCategory entity) {
        if (entity == null) return null;
        CategoryResponse response = new CategoryResponse();
        response.setId(entity.getId());
        if (entity.getDepartment() != null) {
            response.setDepartmentId(entity.getDepartment().getId());
            response.setDepartmentName(entity.getDepartment().getName());
        }
        response.setName(entity.getName());
        response.setDescription(entity.getDescription());
        response.setIsActive(entity.getIsActive());
        if (entity.getCreatedBy() != null) {
            response.setCreatedById(entity.getCreatedBy().getId());
            response.setCreatedByName(entity.getCreatedBy().getFullName().trim());
        }
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }

    public HDCategory toEntity(CategoryCreateRequest request) {
        if (request == null) return null;
        HDCategory entity = new HDCategory();
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        return entity;
    }

    public void updateEntity(HDCategory entity, CategoryUpdateRequest request) {
        if (entity == null || request == null) return;
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        if (request.getIsActive() != null) {
            entity.setIsActive(request.getIsActive());
        }
    }
}
