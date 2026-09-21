package xyz.mobi.employeehelpdesk.service;

import xyz.mobi.employeehelpdesk.entity.Ticket;

public interface TicketRoutingService {

    void routeTicket(Ticket ticket);

    void retryRouting(Ticket ticket);

    void reopenTicket(Long ticketId, Long employeeId);
}