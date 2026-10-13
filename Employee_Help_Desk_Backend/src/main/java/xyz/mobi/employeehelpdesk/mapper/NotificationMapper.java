package xyz.mobi.employeehelpdesk.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import xyz.mobi.employeehelpdesk.dto.notification.NotificationResponseDTO;
import xyz.mobi.employeehelpdesk.entity.Notification;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(source = "id", target = "id")
    @Mapping(source = "recipient.id", target = "recipientId")
    @Mapping(source = "ticket.id", target = "ticketId")
    @Mapping(source = "ticket.ticketNumber", target = "ticketNumber")
    @Mapping(source = "type", target = "type")
    @Mapping(source = "title", target = "title")
    @Mapping(source = "message", target = "message")
    @Mapping(source = "read", target = "read")
    @Mapping(source = "updatedAt", target = "updatedAt")
    @Mapping(target = "createdAt", ignore = true)
    NotificationResponseDTO toUpdateResponse(Notification notification);

    @Mapping(source = "id", target = "id")
    @Mapping(source = "recipient.id", target = "recipientId")
    @Mapping(source = "ticket.id", target = "ticketId")
    @Mapping(source = "ticket.ticketNumber", target = "ticketNumber")
    @Mapping(source = "type", target = "type")
    @Mapping(source = "title", target = "title")
    @Mapping(source = "message", target = "message")
    @Mapping(source = "read", target = "read")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    NotificationResponseDTO toResponse(Notification notification);
}
