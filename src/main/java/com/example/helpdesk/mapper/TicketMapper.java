package com.example.helpdesk.mapper;

import com.example.helpdesk.dto.request.CreateTicketRequest;
import com.example.helpdesk.entity.Category;
import com.example.helpdesk.entity.Department;
import com.example.helpdesk.entity.Employee;
import com.example.helpdesk.entity.SubCategory;
import com.example.helpdesk.entity.Ticket;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

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

}