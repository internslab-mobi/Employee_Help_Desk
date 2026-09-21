package xyz.mobi.employeehelpdesk.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.web.multipart.MultipartFile;
import xyz.mobi.employeehelpdesk.entity.enums.AttachmentType;
import xyz.mobi.employeehelpdesk.entity.Employee;
import xyz.mobi.employeehelpdesk.entity.Ticket;
import xyz.mobi.employeehelpdesk.entity.TicketAttachment;

@Mapper(componentModel = "spring")
public interface TicketAttachmentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(source = "ticket", target = "ticket")
    @Mapping(source = "uploader", target = "uploadedBy")
    @Mapping(
            source = "file.originalFilename",
            target = "originalFilename"
    )
    @Mapping(
            source = "file.contentType",
            target = "mimeType"
    )
    @Mapping(
            source = "file.size",
            target = "fileSize"
    )
    @Mapping(source = "fileData", target = "fileData")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(source = "type", target = "attachmentType")
    TicketAttachment toEntity(
            MultipartFile file,
            Ticket ticket,
            Employee uploader,
            byte[] fileData,
            AttachmentType type
    );

    xyz.mobi.employeehelpdesk.dto.message.TicketAttachmentResponse toResponse(TicketAttachment attachment);
}