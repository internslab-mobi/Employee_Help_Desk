package com.example.helpdesk.service.impl;

import com.example.helpdesk.dto.request.TicketMessageRequest;
import com.example.helpdesk.dto.response.TicketMessageResponse;
import com.example.helpdesk.entity.DepartmentAgent;
import com.example.helpdesk.entity.Employee;
import com.example.helpdesk.entity.Ticket;
import com.example.helpdesk.entity.TicketMessage;
import com.example.helpdesk.enums.NotificationType;
import com.example.helpdesk.enums.TicketEventType;
import com.example.helpdesk.repository.DepartmentAgentRepository;
import com.example.helpdesk.repository.EmployeeRepository;
import com.example.helpdesk.repository.TicketMessageRepository;
import com.example.helpdesk.repository.TicketRepository;
import com.example.helpdesk.service.NotificationService;
import com.example.helpdesk.service.TicketHistoryService;
import com.example.helpdesk.service.TicketMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TicketMessageServiceImpl implements TicketMessageService {

    private final TicketMessageRepository ticketMessageRepository;
    private final TicketRepository ticketRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentAgentRepository departmentAgentRepository;
    private final TicketHistoryService ticketHistoryService;
    private final NotificationService notificationService;

    @Override
    public TicketMessageResponse sendMessage(Long ticketId, TicketMessageRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        Employee sender = employeeRepository.findById(request.getSenderId())
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + request.getSenderId()));

        if (ticket.getAssignedAgent() == null) {
            throw new IllegalStateException("Ticket must have an assigned agent to send messages");
        }

        boolean isRequester = ticket.getRequester().getId().equals(sender.getId());
        boolean isAssignedAgent = ticket.getAssignedAgent().getEmployee().getId().equals(sender.getId());

        if (!isRequester && !isAssignedAgent) {
            throw new IllegalStateException("Sender must be the requester or assigned agent");
        }

        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be blank");
        }

        TicketMessage message = TicketMessage.builder()
                .ticket(ticket)
                .sender(sender)
                .content(request.getContent().trim())
                .seen(false)
                .build();

        message = ticketMessageRepository.save(message);

        ticketHistoryService.recordHistory(
                ticket,
                sender,
                TicketEventType.MESSAGE_SENT,
                null,
                "Message sent by " + sender.getFirstName() + " " + sender.getLastName(),
                null
        );

        Employee recipient = isRequester ? ticket.getAssignedAgent().getEmployee() : ticket.getRequester();
        notificationService.sendNotification(
                recipient,
                ticket,
                NotificationType.NEW_MESSAGE,
                "New Message",
                "You have a new message on ticket " + ticket.getTicketNumber()
        );

        log.info("Message sent for ticket {} by sender {}", ticketId, sender.getId());

        return toResponse(message);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketMessageResponse> getTicketMessages(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        return ticketMessageRepository.findByTicketIdOrderByCreatedAtAsc(ticketId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void markAsSeen(Long messageId) {
        TicketMessage message = ticketMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));

        message.setSeen(true);
        ticketMessageRepository.save(message);

        log.info("Message {} marked as seen", messageId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketMessageResponse> getUnreadMessages(Long ticketId, Long recipientId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        Employee recipient = employeeRepository.findById(recipientId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + recipientId));

        return ticketMessageRepository.findByTicketIdAndSeenFalseOrderByCreatedAtAsc(ticketId)
                .stream()
                .filter(msg -> !msg.getSender().getId().equals(recipientId))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private TicketMessageResponse toResponse(TicketMessage message) {
        return TicketMessageResponse.builder()
                .id(message.getId())
                .ticketId(message.getTicket().getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getFirstName() + " " + message.getSender().getLastName())
                .content(message.getContent())
                .seen(message.getSeen())
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .build();
    }
}
