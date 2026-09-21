package com.example.helpdesk.service;

import com.example.helpdesk.dto.request.TicketMessageRequest;
import com.example.helpdesk.dto.response.TicketMessageResponse;

import java.util.List;

public interface TicketMessageService {

    TicketMessageResponse sendMessage(Long ticketId, TicketMessageRequest request);

    List<TicketMessageResponse> getTicketMessages(Long ticketId);

    void markAsSeen(Long messageId);

    List<TicketMessageResponse> getUnreadMessages(Long ticketId, Long recipientId);
}
