package com.example.helpdesk.mapper;

import com.example.helpdesk.dto.request.CreateTicketRequest;
import com.example.helpdesk.dto.response.TicketResponse;
import com.example.helpdesk.entity.Category;
import com.example.helpdesk.entity.Department;
import com.example.helpdesk.entity.DepartmentAgent;
import com.example.helpdesk.entity.DepartmentManager;
import com.example.helpdesk.entity.Employee;
import com.example.helpdesk.entity.SlaRule;
import com.example.helpdesk.entity.SubCategory;
import com.example.helpdesk.entity.Ticket;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.time.Instant;

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
    @Mapping(target = "requesterName", source = "requester", qualifiedByName = "extractNameFromEmployee")
    @Mapping(target = "departmentId", source = "department", qualifiedByName = "extractIdFromDepartment")
    @Mapping(target = "departmentName", source = "department", qualifiedByName = "extractNameFromDepartment")
    @Mapping(target = "categoryId", source = "category", qualifiedByName = "extractIdFromCategory")
    @Mapping(target = "categoryName", source = "category", qualifiedByName = "extractNameFromCategory")
    @Mapping(target = "subCategoryId", source = "subCategory", qualifiedByName = "extractIdFromSubCategory")
    @Mapping(target = "subCategoryName", source = "subCategory", qualifiedByName = "extractNameFromSubCategory")
    @Mapping(target = "assignedAgentId", source = "assignedAgent", qualifiedByName = "extractIdFromDepartmentAgent")
    @Mapping(target = "assignedAgentName", source = "assignedAgent", qualifiedByName = "extractNameFromDepartmentAgent")
    @Mapping(target = "assignedManagerId", source = "assignedManager", qualifiedByName = "extractIdFromDepartmentManager")
    @Mapping(target = "assignedManagerName", source = "assignedManager", qualifiedByName = "extractNameFromDepartmentManager")
    @Mapping(target = "slaPolicyId", source = "slaPolicy", qualifiedByName = "extractIdFromSlaRule")
    @Mapping(target = "reopenCount", source = "reopenCount")
    @Mapping(target = "resolutionSummary", source = "resolutionSummary")
    @Mapping(target = "holdReason", source = "holdReason")
    @Mapping(target = "holdStartedAt", ignore = true)
    @Mapping(target = "withdrawalReason", source = "withdrawalReason")
    @Mapping(target = "resolvedAt", ignore = true)
    @Mapping(target = "closedAt", ignore = true)
    @Mapping(target = "withdrawnAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "attachments", ignore = true)
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

    @Named("extractIdFromDepartmentManager")
    static Long extractIdFromDepartmentManager(DepartmentManager manager) {
        return manager != null ? manager.getId() : null;
    }

    @Named("extractIdFromSlaRule")
    static Long extractIdFromSlaRule(SlaRule slaRule) {
        return slaRule != null ? slaRule.getId() : null;
    }

    @Named("extractNameFromEmployee")
    static String extractNameFromEmployee(Employee employee) {
        if (employee == null) {
            return null;
        }
        String firstName = employee.getFirstName();
        String lastName = employee.getLastName();
        if (firstName == null && lastName == null) {
            return null;
        }
        if (lastName == null || lastName.isEmpty()) {
            return firstName;
        }
        if (firstName == null || firstName.isEmpty()) {
            return lastName;
        }
        return firstName + " " + lastName;
    }

    @Named("extractNameFromDepartment")
    static String extractNameFromDepartment(Department department) {
        return department != null ? department.getName() : null;
    }

    @Named("extractNameFromCategory")
    static String extractNameFromCategory(Category category) {
        return category != null ? category.getName() : null;
    }

    @Named("extractNameFromSubCategory")
    static String extractNameFromSubCategory(SubCategory subCategory) {
        return subCategory != null ? subCategory.getName() : null;
    }

    @Named("extractNameFromDepartmentAgent")
    static String extractNameFromDepartmentAgent(DepartmentAgent agent) {
        if (agent == null || agent.getEmployee() == null) {
            return null;
        }
        return extractNameFromEmployee(agent.getEmployee());
    }

    @Named("extractNameFromDepartmentManager")
    static String extractNameFromDepartmentManager(DepartmentManager manager) {
        if (manager == null || manager.getEmployee() == null) {
            return null;
        }
        return extractNameFromEmployee(manager.getEmployee());
    }

}