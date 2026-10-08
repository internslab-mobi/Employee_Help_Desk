package xyz.mobi.employeehelpdesk.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import xyz.mobi.employeehelpdesk.dto.message.TicketAttachmentResponseDTO;
import xyz.mobi.employeehelpdesk.dto.message.TicketMessageCreateResponseDTO;
import xyz.mobi.employeehelpdesk.dto.message.TicketMessageResponseDTO;
import xyz.mobi.employeehelpdesk.entity.Employee;
import xyz.mobi.employeehelpdesk.entity.TicketMessage;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TicketMessageMapper {

    @Mapping(source = "message.id", target = "id")
    @Mapping(source = "message.ticket.id", target = "ticketId")
    @Mapping(source = "message.sender.id", target = "senderId")
    @Mapping(target = "senderName", expression = "java(mapSenderName(message.getSender()))")
    @Mapping(source = "message.content", target = "content")
    @Mapping(source = "message.seen", target = "seen")
    @Mapping(source = "message.createdAt", target = "createdAt")
    @Mapping(source = "attachments", target = "attachments")
    TicketMessageCreateResponseDTO toCreateResponse(TicketMessage message, List<TicketAttachmentResponseDTO> attachments);

    @Mapping(source = "message.id", target = "id")
    @Mapping(source = "message.ticket.id", target = "ticketId")
    @Mapping(source = "message.sender.id", target = "senderId")
    @Mapping(target = "senderName", expression = "java(mapSenderName(message.getSender()))")
    @Mapping(source = "message.content", target = "content")
    @Mapping(source = "message.seen", target = "seen")
    @Mapping(source = "attachments", target = "attachments")
    TicketMessageResponseDTO toResponse(TicketMessage message, List<TicketAttachmentResponseDTO> attachments);

    default String mapSenderName(Employee sender) {
        if (sender == null) {
            return null;
        }
        if (sender.getLastName() != null && !sender.getLastName().isBlank()) {
            return sender.getFirstName() + " " + sender.getLastName();
        }
        return sender.getFirstName();
    }
}
