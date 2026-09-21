package xyz.mobi.employeehelpdesk.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import xyz.mobi.employeehelpdesk.dto.ticket.CreateTicketRequest;
import xyz.mobi.employeehelpdesk.dto.ticket.TicketResponse;
import xyz.mobi.employeehelpdesk.entity.SlaInstance;
import xyz.mobi.employeehelpdesk.entity.Ticket;

@Mapper(
        componentModel = "spring",
        builder = @org.mapstruct.Builder(disableBuilder = true)
)
public interface TicketMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ticketNumber", ignore = true)
    @Mapping(target = "requester", ignore = true)
    @Mapping(target = "department", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "subCategory", ignore = true)
    @Mapping(target = "priority", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "assignedAgent", ignore = true)
    @Mapping(target = "assignedManager", ignore = true)
    @Mapping(target = "slaPolicy", ignore = true)
    @Mapping(target = "reopenCount", ignore = true)
    @Mapping(target = "resolutionSummary", ignore = true)
    @Mapping(target = "holdReason", ignore = true)
    @Mapping(target = "holdStartedAt", ignore = true)
    @Mapping(target = "withdrawalReason", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "resolvedAt", ignore = true)
    @Mapping(target = "reopenedAt", ignore = true)
    @Mapping(target = "withdrawnAt", ignore = true)
    Ticket toEntity(CreateTicketRequest request);


    @Mapping(source = "ticket.id", target = "id")
    @Mapping(source = "ticket.requester.id", target = "requesterId")
    @Mapping(source = "ticket.requester.firstName", target = "requesterName")

    @Mapping(source = "ticket.department.id", target = "departmentId")
    @Mapping(source = "ticket.department.name", target = "departmentName")

    @Mapping(source = "ticket.category.id", target = "categoryId")
    @Mapping(source = "ticket.category.name", target = "categoryName")

    @Mapping(source = "ticket.subCategory.id", target = "subCategoryId")
    @Mapping(source = "ticket.subCategory.name", target = "subCategoryName")

    @Mapping(
            source = "ticket.assignedAgent.id",
            target = "assignedAgentId"
    )
    @Mapping(
            source = "ticket.assignedAgent.employee.firstName",
            target = "assignedAgentName"
    )
    @Mapping(
            source = "ticket.assignedManager.employee.id",
            target = "assignedManagerId"
    )
    @Mapping(
            source = "ticket.assignedManager.employee.firstName",
            target = "assignedManagerName"
    )
    @Mapping(source = "slaInstance.status", target = "slaStatus")
    @Mapping(source = "ticket.assignedAt", target = "assignedAt")

    @Mapping(source = "ticket.status", target = "status")
    @Mapping(source = "ticket.createdAt", target = "createdAt")
    @Mapping(source = "ticket.updatedAt", target = "updatedAt")
    @Mapping(source = "ticket.resolvedAt", target = "resolvedAt")
    @Mapping(source = "ticket.reopenedAt", target = "reopenedAt")
    TicketResponse toResponse(Ticket ticket, SlaInstance slaInstance);
}