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

    @Mapping(source = "ticket.id", target = "id")
    @Mapping(source = "ticket.requester", target = "requester")
    @Mapping(source = "ticket.department", target = "department")
    @Mapping(source = "ticket.category", target = "category")
    @Mapping(source = "ticket.subCategory", target = "subCategory")
    @Mapping(source = "ticket.assignedAgent.employee", target = "assignedAgent")
    @Mapping(source = "ticket.assignedManager.id", target = "managerId")
    @Mapping(source = "slaInstance.status", target = "slaStatus")
    @Mapping(source = "ticket.assignedAt", target = "assignedAt")
    @Mapping(source = "ticket.status", target = "status")
    @Mapping(source = "ticket.createdAt", target = "createdAt")
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(source = "slaInstance.currentDeadlineAt", target = "slaDeadline")
    @Mapping(target = "reopenCount", ignore = true)
    @Mapping(target = "resolvedAt", ignore = true)
    @Mapping(target = "reopenedAt", ignore = true)
    TicketResponseDTO toCreateResponse(Ticket ticket, SlaInstance slaInstance);

    @Mapping(source = "ticket.id", target = "id")
    @Mapping(source = "ticket.requester", target = "requester")
    @Mapping(source = "ticket.department", target = "department")
    @Mapping(source = "ticket.category", target = "category")
    @Mapping(source = "ticket.subCategory", target = "subCategory")
    @Mapping(source = "ticket.assignedAgent.employee", target = "assignedAgent")
    @Mapping(source = "ticket.assignedManager.id", target = "managerId")
    @Mapping(source = "slaInstance.status", target = "slaStatus")
    @Mapping(source = "ticket.assignedAt", target = "assignedAt")
    @Mapping(source = "ticket.status", target = "status")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(source = "ticket.updatedAt", target = "updatedAt")
    @Mapping(source = "ticket.resolvedAt", target = "resolvedAt")
    @Mapping(source = "ticket.reopenedAt", target = "reopenedAt")
    @Mapping(target = "slaDeadline", ignore = true)
    TicketResponseDTO toUpdateResponse(Ticket ticket, SlaInstance slaInstance);

    @Mapping(source = "ticket.id", target = "id")
    @Mapping(source = "ticket.requester", target = "requester")
    @Mapping(source = "ticket.department", target = "department")
    @Mapping(source = "ticket.category", target = "category")
    @Mapping(source = "ticket.subCategory", target = "subCategory")
    @Mapping(source = "ticket.assignedAgent.employee", target = "assignedAgent")
    @Mapping(source = "ticket.assignedManager.id", target = "managerId")
    @Mapping(source = "slaInstance.status", target = "slaStatus")
    @Mapping(source = "ticket.assignedAt", target = "assignedAt")
    @Mapping(source = "ticket.status", target = "status")
    @Mapping(source = "ticket.resolvedAt", target = "resolvedAt")
    @Mapping(source = "ticket.reopenedAt", target = "reopenedAt")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "slaDeadline", ignore = true)
    TicketResponseDTO toResponse(Ticket ticket, SlaInstance slaInstance);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", expression = "java(mapEmployeeName(employee))")
    IdNameResponseDTO toIdNameResponse(Employee employee);

    IdNameResponseDTO toIdNameResponse(Department department);

    IdNameResponseDTO toIdNameResponse(Category category);

    IdNameResponseDTO toIdNameResponse(SubCategory subCategory);

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