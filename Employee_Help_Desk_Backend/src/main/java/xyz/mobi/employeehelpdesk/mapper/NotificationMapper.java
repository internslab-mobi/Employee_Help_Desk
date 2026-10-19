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
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "updatedAt", target = "updatedAt")
    NotificationResponseDTO toUpdateResponse(Notification notification);

    @Mapping(source = "id", target = "id")
    @Mapping(source = "recipient.id", target = "recipientId")
    @Mapping(source = "ticket.id", target = "ticketId")
    @Mapping(source = "ticket.ticketNumber", target = "ticketNumber")
    @Mapping(source = "type", target = "type")
    @Mapping(source = "title", target = "title")
    @Mapping(source = "message", target = "message")
    @Mapping(source = "read", target = "read")
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "updatedAt", target = "updatedAt")
    NotificationResponseDTO toResponse(Notification notification);
}
