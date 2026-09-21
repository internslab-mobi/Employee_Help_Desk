package xyz.mobi.employeehelpdesk.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import xyz.mobi.employeehelpdesk.dto.message.TicketAttachmentResponse;
import xyz.mobi.employeehelpdesk.dto.message.TicketMessageResponse;
import xyz.mobi.employeehelpdesk.entity.Employee;
import xyz.mobi.employeehelpdesk.entity.Ticket;
import xyz.mobi.employeehelpdesk.entity.TicketAttachment;
import xyz.mobi.employeehelpdesk.entity.TicketMessage;
import xyz.mobi.employeehelpdesk.entity.enums.AttachmentType;
import xyz.mobi.employeehelpdesk.entity.enums.HistoryEventType;
import xyz.mobi.employeehelpdesk.entity.enums.NotificationType;
import xyz.mobi.employeehelpdesk.entity.enums.TicketStatus;
import xyz.mobi.employeehelpdesk.exception.BadRequestException;
import xyz.mobi.employeehelpdesk.exception.ResourceNotFoundException;
import xyz.mobi.employeehelpdesk.mapper.TicketAttachmentMapper;
import xyz.mobi.employeehelpdesk.mapper.TicketMessageMapper;
import xyz.mobi.employeehelpdesk.repository.EmployeeRepository;
import xyz.mobi.employeehelpdesk.repository.TicketAttachmentRepository;
import xyz.mobi.employeehelpdesk.repository.TicketMessageRepository;
import xyz.mobi.employeehelpdesk.repository.TicketRepository;
import xyz.mobi.employeehelpdesk.service.CurrentUserService;
import xyz.mobi.employeehelpdesk.service.NotificationService;
import xyz.mobi.employeehelpdesk.service.TicketMessageService;
import xyz.mobi.employeehelpdesk.service.helperservice.TicketHistoryService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketMessageServiceImpl implements TicketMessageService {

    private final TicketRepository ticketRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final TicketAttachmentRepository ticketAttachmentRepository;
    private final EmployeeRepository employeeRepository;
    private final TicketHistoryService ticketHistoryService;
    private final NotificationService notificationService;
    private final TicketMessageMapper ticketMessageMapper;
    private final TicketAttachmentMapper ticketAttachmentMapper;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TicketMessageResponse createMessage(
            Long ticketId,
            String content,
            List<MultipartFile> attachments
    ) throws IOException {
        long currentEmployeeId = currentUserService.getCurrentEmployeeId();

        if (content == null || content.isBlank()) {
            throw new BadRequestException("Message content is required");
        }

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));

        if (ticket.getStatus() == TicketStatus.RESOLVED || ticket.getStatus() == TicketStatus.WITHDRAWN) {
            throw new BadRequestException("Messages are not allowed on resolved or withdrawn tickets");
        }

        boolean isRequester = ticket.getRequester().getId().equals(currentEmployeeId);
        boolean isAssignedAgent = ticket.getAssignedAgent() != null
                && (ticket.getAssignedAgent().getEmployee().getId().equals(currentEmployeeId)
                || ticket.getAssignedAgent().getId().equals(currentEmployeeId));

        if (!isRequester && !isAssignedAgent) {
            throw new AccessDeniedException("You are not allowed to send messages on this ticket");
        }

        Employee sender = employeeRepository.findById(currentEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + currentEmployeeId));

        TicketMessage message = new TicketMessage();
        message.setTicket(ticket);
        message.setSender(sender);
        message.setContent(content);
        message.setSeen(false);

        message = ticketMessageRepository.save(message);

        List<TicketAttachmentResponse> attachmentResponses = new ArrayList<>();
        boolean hasSavedAttachments = false;

        if (attachments != null && !attachments.isEmpty()) {
            for (MultipartFile file : attachments) {
                if (file.isEmpty()) {
                    continue;
                }
                byte[] fileData = file.getBytes();
                TicketAttachment attachment = ticketAttachmentMapper.toEntity(
                        file,
                        ticket,
                        sender,
                        fileData,
                        AttachmentType.TICKET_MESSAGE
                );
                attachment.setMessage(message);
                attachment = ticketAttachmentRepository.save(attachment);
                attachmentResponses.add(ticketAttachmentMapper.toResponse(attachment));
                hasSavedAttachments = true;
            }
        }

        if (hasSavedAttachments) {
            ticketHistoryService.record(
                    ticket,
                    HistoryEventType.ATTACHMENT_ADDED,
                    ticket.getStatus(),
                    ticket.getStatus()
            );
        }

        ticketHistoryService.record(
                ticket,
                HistoryEventType.MESSAGE_ADDED,
                ticket.getStatus(),
                ticket.getStatus()
            );

        String ticketNumber = ticket.getTicketNumber() != null ? ticket.getTicketNumber() : ("#" + ticket.getId());
        String notifMessage = "You have a new message on ticket " + ticketNumber + ".";

        if (isRequester) {
            if (ticket.getAssignedAgent() != null && ticket.getAssignedAgent().getEmployee() != null) {
                notificationService.sendNotification(
                        ticket.getAssignedAgent().getEmployee(),
                        ticket,
                        NotificationType.NEW_MESSAGE,
                        "New Ticket Message",
                        notifMessage
                );
            }
        } else {
            notificationService.sendNotification(
                    ticket.getRequester(),
                    ticket,
                    NotificationType.NEW_MESSAGE,
                    "New Ticket Message",
                    notifMessage
            );
        }

        return ticketMessageMapper.toResponse(message, attachmentResponses);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TicketMessageResponse> getMessages(Long ticketId, Pageable pageable) {
        long currentEmployeeId = currentUserService.getCurrentEmployeeId();

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));

        validateTicketAccess(ticket, currentEmployeeId);

        Page<TicketMessage> messagesPage = ticketMessageRepository.findByTicketIdOrderByCreatedAtAsc(ticketId, pageable);

        List<Long> messageIds = messagesPage.getContent().stream()
                .map(TicketMessage::getId)
                .toList();

        Map<Long, List<TicketAttachmentResponse>> attachmentMap;
        if (messageIds.isEmpty()) {
            attachmentMap = Collections.emptyMap();
        } else {
            List<TicketAttachment> attachments = ticketAttachmentRepository.findByMessageIdIn(messageIds);
            attachmentMap = attachments.stream()
                    .collect(Collectors.groupingBy(
                            a -> a.getMessage().getId(),
                            Collectors.mapping(ticketAttachmentMapper::toResponse, Collectors.toList())
                    ));
        }

        List<TicketMessageResponse> responseList = messagesPage.getContent().stream()
                .map(m -> ticketMessageMapper.toResponse(m, attachmentMap.getOrDefault(m.getId(), Collections.emptyList())))
                .toList();

        return new PageImpl<>(responseList, pageable, messagesPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public TicketAttachment getAttachmentForDownload(Long ticketId, Long attachmentId) {
        long currentEmployeeId = currentUserService.getCurrentEmployeeId();

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));

        validateTicketAccess(ticket, currentEmployeeId);

        TicketAttachment attachment = ticketAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found: " + attachmentId));

        if (!attachment.getTicket().getId().equals(ticketId)) {
            throw new BadRequestException("Attachment does not belong to this ticket");
        }

        return attachment;
    }

    private void validateTicketAccess(Ticket ticket, long currentEmployeeId) {
        boolean isRequester = ticket.getRequester().getId().equals(currentEmployeeId);
        boolean isAssignedAgent = ticket.getAssignedAgent() != null
                && (ticket.getAssignedAgent().getEmployee().getId().equals(currentEmployeeId)
                || ticket.getAssignedAgent().getId().equals(currentEmployeeId));

        if (!isRequester && !isAssignedAgent) {
            throw new AccessDeniedException("You are not allowed to access messages for this ticket");
        }
    }
}
