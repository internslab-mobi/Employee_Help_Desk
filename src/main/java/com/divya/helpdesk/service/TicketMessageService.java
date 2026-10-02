package com.divya.helpdesk.service;

import com.divya.helpdesk.dto.ticket.TicketMessageResponseDTO;
import com.divya.helpdesk.entity.HDEmployeeEntity;
import com.divya.helpdesk.entity.HDTicketEntity;
import com.divya.helpdesk.entity.HDTicketMessageEntity;

import java.util.List;

public interface TicketMessageService {

    HDTicketMessageEntity sendMessage(HDTicketEntity ticket, HDEmployeeEntity sender, String message);

    List<TicketMessageResponseDTO> getMessages(Long ticketId);
}