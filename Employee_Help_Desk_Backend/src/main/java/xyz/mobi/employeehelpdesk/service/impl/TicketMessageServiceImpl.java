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
import xyz.mobi.employeehelpdesk.dto.message.TicketAttachmentResponseDTO;
import xyz.mobi.employeehelpdesk.dto.message.TicketMessageResponseDTO;
import xyz.mobi.employeehelpdesk.entity.Employee;
import xyz.mobi.employeehelpdesk.entity.Ticket;
import xyz.mobi.employeehelpdesk.entity.TicketAttachment;
import xyz.mobi.employeehelpdesk.entity.TicketMessage;
import xyz.mobi.employeehelpdesk.entity.enums.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import xyz.mobi.employeehelpdesk.exception.BadRequestException;
import xyz.mobi.employeehelpdesk.exception.InvalidStateException;
import xyz.mobi.employeehelpdesk.exception.ResourceNotFoundException;
import xyz.mobi.employeehelpdesk.exception.UnauthorizedException;
import xyz.mobi.employeehelpdesk.mapper.TicketAttachmentMapper;
import xyz.mobi.employeehelpdesk.mapper.TicketMessageMapper;
import xyz.mobi.employeehelpdesk.repository.EmployeeRepository;
import xyz.mobi.employeehelpdesk.repository.TicketAttachmentRepository;
import xyz.mobi.employeehelpdesk.repository.TicketMessageRepository;
import xyz.mobi.employeehelpdesk.repository.TicketRepository;
import xyz.mobi.employeehelpdesk.security.UserPrincipal;
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

    private long getCurrentEmployeeId(UserPrincipal userPrincipal) {
        if (userPrincipal != null && userPrincipal.getEmployeeId() != null) {
            return userPrincipal.getEmployeeId();
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal up && up.getEmployeeId() != null) {
            return up.getEmployeeId();
        }
        throw new UnauthorizedException("User is not authenticated");
    }

    private UserRole getCurrentUserRole(UserPrincipal userPrincipal) {
        if (userPrincipal != null && userPrincipal.getRole() != null) {
            return userPrincipal.getRole();
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal up && up.getRole() != null) {
            return up.getRole();
        }
        throw new UnauthorizedException("User is not authenticated");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TicketMessageResponseDTO createMessage(
            Long ticketId,
            String content,
            List<MultipartFile> attachments,
            UserPrincipal userPrincipal
    ) throws IOException {
        long currentEmployeeId = getCurrentEmployeeId(userPrincipal);

        if (content == null || content.isBlank()) {
            throw new BadRequestException("Message content is required");
        }

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));

        if (ticket.getStatus() == TicketStatus.OPEN || ticket.getStatus() == TicketStatus.REOPENED) {
            throw new InvalidStateException("Ticket is not started yet. Messages and attachments can only be sent once the ticket is in progress.");
        }

        if (ticket.getStatus() == TicketStatus.RESOLVED || ticket.getStatus() == TicketStatus.WITHDRAWN) {
            throw new InvalidStateException("Messages and attachments are not allowed on resolved or withdrawn tickets");
        }

        boolean isRequester = ticket.getRequester() != null
                && ticket.getRequester().getId().equals(currentEmployeeId);
        boolean isAssignedAgent = ticket.getAssignedAgent() != null
                && ticket.getAssignedAgent().getEmployee() != null
                && ticket.getAssignedAgent().getEmployee().getId().equals(currentEmployeeId);

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

        List<TicketAttachmentResponseDTO> attachmentResponses = new ArrayList<>();
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
                        ticket.getAssignedAgent().getEmployee().getId(),
                        ticket.getId(),
                        NotificationType.NEW_MESSAGE,
                        "New Ticket Message",
                        notifMessage
                );
            }
        } else {
            notificationService.sendNotification(
                    ticket.getRequester().getId(),
                    ticket.getId(),
                    NotificationType.NEW_MESSAGE,
                    "New Ticket Message",
                    notifMessage
            );
        }

        log.info("Ticket message created: messageId={}, ticketId={}, senderId={}, attachmentCount={}",
                message.getId(), ticketId, currentEmployeeId, attachmentResponses.size());

        return ticketMessageMapper.toCreateResponse(message, attachmentResponses);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TicketMessageResponseDTO createMessage(
            Long ticketId,
            String content,
            List<MultipartFile> attachments
    ) throws IOException {
        return createMessage(ticketId, content, attachments, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TicketMessageResponseDTO> getMessages(Long ticketId, Pageable pageable, UserPrincipal userPrincipal) {
        long currentEmployeeId = getCurrentEmployeeId(userPrincipal);
        UserRole role = getCurrentUserRole(userPrincipal);

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));

        validateTicketAccess(ticket, currentEmployeeId, role);

        Page<TicketMessage> messagesPage = ticketMessageRepository.findByTicketIdOrderByCreatedAtAsc(ticketId, pageable);

        List<Long> messageIds = messagesPage.getContent().stream()
                .map(TicketMessage::getId)
                .toList();

        Map<Long, List<TicketAttachmentResponseDTO>> attachmentMap;
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

        List<TicketMessageResponseDTO> responseList = messagesPage.getContent().stream()
                .map(m -> ticketMessageMapper.toResponse(m, attachmentMap.getOrDefault(m.getId(), Collections.emptyList())))
                .toList();

        return new PageImpl<>(responseList, pageable, messagesPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TicketMessageResponseDTO> getMessages(Long ticketId, Pageable pageable) {
        return getMessages(ticketId, pageable, null);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketAttachment getAttachmentForDownload(Long ticketId, Long attachmentId, UserPrincipal userPrincipal) {
        long currentEmployeeId = getCurrentEmployeeId(userPrincipal);
        UserRole role = getCurrentUserRole(userPrincipal);

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));

        validateTicketAccess(ticket, currentEmployeeId, role);

        TicketAttachment attachment = ticketAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found: " + attachmentId));

        if (!attachment.getTicket().getId().equals(ticketId)) {
            throw new BadRequestException("Attachment does not belong to this ticket");
        }

        return attachment;
    }

    @Override
    @Transactional(readOnly = true)
    public TicketAttachment getAttachmentForDownload(Long ticketId, Long attachmentId) {
        return getAttachmentForDownload(ticketId, attachmentId, null);
    }

    private void validateTicketAccess(Ticket ticket, long currentEmployeeId, UserRole role) {
        if (role == UserRole.ADMIN) {
            return;
        }

        boolean isRequester = ticket.getRequester() != null
                && ticket.getRequester().getId().equals(currentEmployeeId);
        boolean isAssignedAgent = ticket.getAssignedAgent() != null
                && ticket.getAssignedAgent().getEmployee() != null
                && ticket.getAssignedAgent().getEmployee().getId().equals(currentEmployeeId);

        if (isRequester || isAssignedAgent) {
            return;
        }

        if (role == UserRole.MANAGER) {
            Employee currentEmp = employeeRepository.findById(currentEmployeeId).orElse(null);
            if (currentEmp != null && currentEmp.getDepartment() != null) {
                Long deptId = currentEmp.getDepartment().getId();
                if ((ticket.getDepartment() != null && deptId.equals(ticket.getDepartment().getId()))
                        || (ticket.getRequester() != null && ticket.getRequester().getDepartment() != null && deptId.equals(ticket.getRequester().getDepartment().getId()))
                        || (ticket.getAssignedAgent() != null && ticket.getAssignedAgent().getEmployee() != null && ticket.getAssignedAgent().getEmployee().getDepartment() != null && deptId.equals(ticket.getAssignedAgent().getEmployee().getDepartment().getId()))) {
                    return;
                }
            }
        }

        throw new AccessDeniedException("You are not allowed to access messages for this ticket");
    }
}
