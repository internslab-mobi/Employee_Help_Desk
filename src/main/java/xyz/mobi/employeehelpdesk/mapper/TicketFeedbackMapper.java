package xyz.mobi.employeehelpdesk.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import xyz.mobi.employeehelpdesk.dto.feedback.TicketFeedbackResponseDto;
import xyz.mobi.employeehelpdesk.entity.Employee;
import xyz.mobi.employeehelpdesk.entity.TicketFeedback;

@Mapper(componentModel = "spring")
public interface TicketFeedbackMapper {

    @Mapping(source = "id", target = "id")
    @Mapping(source = "ticket.id", target = "ticketId")
    @Mapping(source = "submittedBy.id", target = "submittedById")
    @Mapping(target = "submittedByName", expression = "java(mapEmployeeName(feedback.getSubmittedBy()))")
    @Mapping(source = "rating", target = "rating")
    @Mapping(source = "comment", target = "comment")
    @Mapping(source = "createdAt", target = "createdAt")
    TicketFeedbackResponseDto toResponse(TicketFeedback feedback);

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
