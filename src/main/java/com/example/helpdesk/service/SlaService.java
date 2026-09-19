package com.example.helpdesk.service;

import com.example.helpdesk.entity.Ticket;
import com.example.helpdesk.entity.TicketSla;

public interface SlaService {

    TicketSla createSlaInstance(Ticket ticket);

    void updateSlaStatus(Ticket ticket);

    void pauseSla(Long ticketId);

    void resumeSla(Long ticketId);

    void completeSla(Long ticketId);

    void checkAndNotifySlaBreaches();
}
