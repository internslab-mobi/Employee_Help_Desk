package com.example.helpdesk.mapper;

import com.example.helpdesk.dto.request.CreateTicketRequest;
import com.example.helpdesk.dto.response.TicketResponse;
import com.example.helpdesk.entity.Category;
import com.example.helpdesk.entity.Department;
import com.example.helpdesk.entity.DepartmentAgent;
import com.example.helpdesk.entity.Employee;
import com.example.helpdesk.entity.SlaRule;
import com.example.helpdesk.entity.SubCategory;
import com.example.helpdesk.entity.Ticket;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface TicketMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "requester", source = "requester")
    @Mapping(target = "department", source = "department")
    @Mapping(target = "category", source = "category")
    @Mapping(target = "subCategory", source = "subCategory")
    @Mapping(target = "subject", source = "request.subject")
    @Mapping(target = "description", source = "request.description")
    @Mapping(target = "ticketNumber", source = "ticketNumber")
    @Mapping(target = "priority", source = "priority")
    @Mapping(target = "status", source = "request.status")
    @Mapping(target = "reopenCount", constant = "0")
    @Mapping(target = "assignedAgent", ignore = true)
    @Mapping(target = "assignedManager", ignore = true)
    @Mapping(target = "slaPolicy", ignore = true)
    @Mapping(target = "resolutionSummary", ignore = true)
    @Mapping(target = "holdReason", ignore = true)
    @Mapping(target = "holdStartedAt", ignore = true)
    @Mapping(target = "withdrawalReason", ignore = true)
    @Mapping(target = "resolvedAt", ignore = true)
    @Mapping(target = "reopenedAt", ignore = true)
    @Mapping(target = "closedAt", ignore = true)
    @Mapping(target = "withdrawnAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Ticket toEntity(
            CreateTicketRequest request,
            Employee requester,
            Department department,
            Category category,
            SubCategory subCategory,
            String ticketNumber,
            String priority
    );

    @Mapping(target = "requesterId", source = "requester", qualifiedByName = "extractIdFromEmployee")
    @Mapping(target = "requesterName", ignore = true)
    @Mapping(target = "departmentId", source = "department", qualifiedByName = "extractIdFromDepartment")
    @Mapping(target = "departmentName", ignore = true)
    @Mapping(target = "categoryId", source = "category", qualifiedByName = "extractIdFromCategory")
    @Mapping(target = "categoryName", ignore = true)
    @Mapping(target = "subCategoryId", source = "subCategory", qualifiedByName = "extractIdFromSubCategory")
    @Mapping(target = "subCategoryName", ignore = true)
    @Mapping(target = "assignedAgentId", source = "assignedAgent", qualifiedByName = "extractIdFromDepartmentAgent")
    @Mapping(target = "assignedAgentName", ignore = true)
    @Mapping(target = "assignedManagerId", source = "assignedManager", qualifiedByName = "extractIdFromEmployee")
    @Mapping(target = "assignedManagerName", ignore = true)
    @Mapping(target = "slaPolicyId", source = "slaPolicy", qualifiedByName = "extractIdFromSlaRule")
    @Mapping(target = "reopenCount", source = "reopenCount")
    @Mapping(target = "resolutionSummary", source = "resolutionSummary")
    @Mapping(target = "holdReason", source = "holdReason")
    @Mapping(target = "holdStartedAt", source = "holdStartedAt")
    @Mapping(target = "withdrawalReason", source = "withdrawalReason")
    @Mapping(target = "resolvedAt", source = "resolvedAt")
    @Mapping(target = "closedAt", source = "closedAt")
    @Mapping(target = "withdrawnAt", source = "withdrawnAt")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    TicketResponse toResponse(Ticket ticket);

    @Named("extractIdFromEmployee")
    static Long extractIdFromEmployee(Employee employee) {
        return employee != null ? employee.getId() : null;
    }

    @Named("extractIdFromDepartment")
    static Long extractIdFromDepartment(Department department) {
        return department != null ? department.getId() : null;
    }

    @Named("extractIdFromCategory")
    static Long extractIdFromCategory(Category category) {
        return category != null ? category.getId() : null;
    }

    @Named("extractIdFromSubCategory")
    static Long extractIdFromSubCategory(SubCategory subCategory) {
        return subCategory != null ? subCategory.getId() : null;
    }

    @Named("extractIdFromDepartmentAgent")
    static Long extractIdFromDepartmentAgent(DepartmentAgent agent) {
        return agent != null ? agent.getId() : null;
    }

    @Named("extractIdFromSlaRule")
    static Long extractIdFromSlaRule(SlaRule slaRule) {
        return slaRule != null ? slaRule.getId() : null;
    }
}