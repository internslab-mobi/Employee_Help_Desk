package com.divya.helpdesk.service;

import com.divya.helpdesk.dto.ticket.TicketHistoryResponse;
import com.divya.helpdesk.entity.HDEmployee;
import com.divya.helpdesk.entity.HDTicket;
import com.divya.helpdesk.entity.HDTicketHistory;
import com.divya.helpdesk.enums.TicketEventType;

import java.util.List;

public interface TicketHistoryService {

    void log(HDTicket ticket, HDEmployee actor, TicketEventType eventType, String oldValue, String newValue);

    List<TicketHistoryResponse> getHistory(Long ticketId);
}