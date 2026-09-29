package xyz.mobi.employeehelpdesk.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import xyz.mobi.employeehelpdesk.dto.ticket.*;
import xyz.mobi.employeehelpdesk.entity.*;

@Mapper(
        componentModel = "spring",
        builder = @org.mapstruct.Builder(disableBuilder = true)
)
public interface TicketMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "ticketNumber", ignore = true)
    @Mapping(target = "requester", ignore = true)
    @Mapping(target = "department", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "subCategory", ignore = true)
    @Mapping(target = "priority", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "assignedAgent", ignore = true)
    @Mapping(target = "assignedAt", ignore = true)
    @Mapping(target = "slaPolicy", ignore = true)
    @Mapping(target = "reopenCount", ignore = true)
    @Mapping(target = "resolutionSummary", ignore = true)
    @Mapping(target = "holdReason", ignore = true)
    @Mapping(target = "holdStartedAt", ignore = true)
    @Mapping(target = "withdrawalReason", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "managerId", ignore = true)
    @Mapping(target = "resolvedAt", ignore = true)
    @Mapping(target = "reopenedAt", ignore = true)
    @Mapping(target = "withdrawnAt", ignore = true)
    @Mapping(target = "reopenReason", ignore = true)
    Ticket toEntity(CreateTicketRequest request);

    @Mapping(source = "ticket.id", target = "id")
    @Mapping(source = "ticket.requester", target = "requester")
    @Mapping(source = "ticket.department", target = "department")
    @Mapping(source = "ticket.category", target = "category")
    @Mapping(source = "ticket.subCategory", target = "subCategory")
    @Mapping(source = "ticket.assignedAgent.employee", target = "assignedAgent")
    @Mapping(source = "ticket.managerId", target = "managerId")
    @Mapping(source = "slaInstance.status", target = "slaStatus")
    @Mapping(source = "ticket.assignedAt", target = "assignedAt")
    @Mapping(source = "ticket.status", target = "status")
    @Mapping(source = "ticket.createdAt", target = "createdAt")
    @Mapping(source = "ticket.resolvedAt", target = "resolvedAt")
    @Mapping(source = "ticket.reopenedAt", target = "reopenedAt")
    TicketCreateResponse toCreateResponse(Ticket ticket, SlaInstance slaInstance);

    @Mapping(source = "ticket.id", target = "id")
    @Mapping(source = "ticket.requester", target = "requester")
    @Mapping(source = "ticket.department", target = "department")
    @Mapping(source = "ticket.category", target = "category")
    @Mapping(source = "ticket.subCategory", target = "subCategory")
    @Mapping(source = "ticket.assignedAgent.employee", target = "assignedAgent")
    @Mapping(source = "ticket.managerId", target = "managerId")
    @Mapping(source = "slaInstance.status", target = "slaStatus")
    @Mapping(source = "ticket.assignedAt", target = "assignedAt")
    @Mapping(source = "ticket.status", target = "status")
    @Mapping(source = "ticket.updatedAt", target = "updatedAt")
    @Mapping(source = "ticket.resolvedAt", target = "resolvedAt")
    @Mapping(source = "ticket.reopenedAt", target = "reopenedAt")
    TicketUpdateResponse toUpdateResponse(Ticket ticket, SlaInstance slaInstance);

    @Mapping(source = "ticket.id", target = "id")
    @Mapping(source = "ticket.requester", target = "requester")
    @Mapping(source = "ticket.department", target = "department")
    @Mapping(source = "ticket.category", target = "category")
    @Mapping(source = "ticket.subCategory", target = "subCategory")
    @Mapping(source = "ticket.assignedAgent.employee", target = "assignedAgent")
    @Mapping(source = "ticket.managerId", target = "managerId")
    @Mapping(source = "slaInstance.status", target = "slaStatus")
    @Mapping(source = "ticket.assignedAt", target = "assignedAt")
    @Mapping(source = "ticket.status", target = "status")
    @Mapping(source = "ticket.resolvedAt", target = "resolvedAt")
    @Mapping(source = "ticket.reopenedAt", target = "reopenedAt")
    TicketResponse toResponse(Ticket ticket, SlaInstance slaInstance);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", expression = "java(mapEmployeeName(employee))")
    IdNameResponse toIdNameResponse(Employee employee);

    IdNameResponse toIdNameResponse(Department department);

    IdNameResponse toIdNameResponse(Category category);

    IdNameResponse toIdNameResponse(SubCategory subCategory);

    default String mapEmployeeName(Employee employee) {
        if (employee == null) {
            return null;
        }
        if (employee.getLastName() != null && !employee.getLastName().isBlank()) {
            return employee.getFirstName() + " " + employee.getLastName();
        }
        return employee.getFirstName();
    }
}