package com.divya.helpdesk.service;

import com.divya.helpdesk.dto.ticket.TicketHistoryResponseDTO;
import com.divya.helpdesk.entity.HDEmployeeEntity;
import com.divya.helpdesk.entity.HDTicketEntity;
import com.divya.helpdesk.enums.TicketEventType;

import java.util.List;

public interface TicketHistoryService {

    void log(HDTicketEntity ticket, HDEmployeeEntity actor, TicketEventType eventType, String oldValue, String newValue);

    List<TicketHistoryResponseDTO> getHistory(Long ticketId);
}