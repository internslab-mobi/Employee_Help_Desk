package com.divya.helpdesk.mapper;

import com.divya.helpdesk.dto.ticket.AgentDTO;
import com.divya.helpdesk.dto.ticket.CategoryDTO;
import com.divya.helpdesk.dto.ticket.CreateTicketResponseDTO;
import com.divya.helpdesk.dto.ticket.SubCategoryDTO;
import com.divya.helpdesk.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {EmployeeMapper.class})
public interface TicketMapper {

    @Mapping(target = "ticketId", source = "ticket.id")
    @Mapping(target = "requester", source = "ticket.requester")
    @Mapping(target = "department", source = "ticket.department")
    @Mapping(target = "category", source = "ticket.category")
    @Mapping(target = "subCategory", source = "ticket.subCategory")
    @Mapping(target = "agent", source = "ticket.assignedAgent")
    @Mapping(target = "manager", source = "ticket.assignedManager")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "workStartedAt", ignore = true)
    @Mapping(target = "warningAt", ignore = true)
    @Mapping(target = "resolvedAt", ignore = true)
    @Mapping(target = "withdrawnAt", ignore = true)
    CreateTicketResponseDTO mapToResponse(HDTicketEntity ticket);


    @Mapping(target = "name", expression = "java(mapFullName(employee))")
    AgentDTO toEmployeeResponse(HDEmployeeEntity employee);

    CategoryDTO toCategoryResponse(HDCategoryEntity category);

    SubCategoryDTO toSubCategoryResponse(HDSubCategoryEntity subCategory);

    default String mapFullName(HDEmployeeEntity employee) {
        if (employee == null) {
            return null;
        }
        String first = employee.getFirstName() != null ? employee.getFirstName() : "";
        String last = employee.getLastName() != null ? employee.getLastName() : "";
        return (first + " " + last).trim();
    }
}
