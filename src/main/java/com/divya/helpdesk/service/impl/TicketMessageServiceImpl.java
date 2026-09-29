package com.divya.helpdesk.service.impl;

import com.divya.helpdesk.dto.ticket.MessageSenderDTO;
import com.divya.helpdesk.dto.ticket.TicketMessageResponse;
import com.divya.helpdesk.entity.HDEmployee;
import com.divya.helpdesk.entity.HDTicket;
import com.divya.helpdesk.entity.HDTicketMessage;
import com.divya.helpdesk.enums.NotificationType;
import com.divya.helpdesk.repository.HDTicketMessageRepository;
import com.divya.helpdesk.service.EmailService;
import com.divya.helpdesk.service.NotificationService;
import com.divya.helpdesk.service.TicketMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TicketMessageServiceImpl implements TicketMessageService {

    private final HDTicketMessageRepository messageRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;

    @Override
    public HDTicketMessage sendMessage(HDTicket ticket, HDEmployee sender, String message) {
        if (ticket == null || sender == null || message == null || message.isBlank()) {
            throw new IllegalArgumentException("Ticket, sender, and message text must not be empty");
        }

        HDTicketMessage ticketMessage = new HDTicketMessage();
        ticketMessage.setTicket(ticket);
        ticketMessage.setSender(sender);
        ticketMessage.setMessageText(message.trim());

        HDTicketMessage saved = messageRepository.save(ticketMessage);

        // Determine recipient
        HDEmployee recipient = null;
        if (ticket.getRequester() != null && ticket.getRequester().getId().equals(sender.getId())) {
            // Requester sent the message -> notify assigned agent or manager
            recipient = ticket.getAssignedAgent() != null ? ticket.getAssignedAgent() : ticket.getAssignedManager();
        } else if (ticket.getRequester() != null) {
            // Agent/Manager sent the message -> notify requester
            recipient = ticket.getRequester();
        }

        if (recipient != null) {
            emailService.sendNewMessageNotificationEmail(ticket, saved, recipient);
            notificationService.createNotification(
                    recipient,
                    "New message on ticket " + ticket.getTicketNumber(),
                    sender.getFirstName() + ": " + (message.length() > 60 ? message.substring(0, 57) + "..." : message),
                    NotificationType.STATUS_CHANGED, ticket);
        }

        log.info("Message sent on ticket {} by user ID {}", ticket.getTicketNumber(), sender.getId());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketMessageResponse> getMessages(Long ticketId) {
        return messageRepository.findByTicket_IdOrderByCreatedAtAsc(ticketId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    private TicketMessageResponse mapToResponse(HDTicketMessage message) {
        return TicketMessageResponse.builder()
                .id(message.getId())
                .ticketId(message.getTicket() != null ? message.getTicket().getId() : null)
                .sender(mapToSender(message.getSender()))
                .messageText(message.getMessageText())
                .createdAt(message.getCreatedAt().atZone(ZoneId.of(message.getSender().getTimezone())).toOffsetDateTime())
                .build();
    }

    private MessageSenderDTO mapToSender(HDEmployee employee){
        if(employee == null) return null;

        return MessageSenderDTO.builder()
                .id(employee.getId())
                .name(employee.getFirstName() + " " + employee.getLastName())
                .role(employee.getRole().toString())
                .build();
    }
}