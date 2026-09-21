package com.divya.helpdesk.service;

import com.divya.helpdesk.entity.HDTicket;

public interface TicketAssignmentService {
    void assignAgentOrEscalate(HDTicket ticket);
    int calculateAgentWorkload(Long employeeId);
}
