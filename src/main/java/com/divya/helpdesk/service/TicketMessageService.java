package com.divya.helpdesk.service;

import com.divya.helpdesk.dto.ticket.TicketMessageResponse;
import com.divya.helpdesk.entity.HDEmployee;
import com.divya.helpdesk.entity.HDTicket;
import com.divya.helpdesk.entity.HDTicketMessage;

import java.util.List;

public interface TicketMessageService {

    HDTicketMessage sendMessage(HDTicket ticket, HDEmployee sender, String message);

    List<TicketMessageResponse> getMessages(Long ticketId);
}