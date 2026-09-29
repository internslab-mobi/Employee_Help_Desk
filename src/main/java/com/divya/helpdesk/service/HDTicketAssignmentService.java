package com.divya.helpdesk.service;

import com.divya.helpdesk.entity.HDEmployee;
import com.divya.helpdesk.entity.HDTicket;

public interface HDTicketAssignmentService {

    /**
     * Automatically selects and assigns the optimal agent for a ticket based on
     * department, skill matching, weighted active workload, and last assignment timestamp.
     *
     * @param ticket the ticket to assign
     * @return the assigned HDEmployee agent, or null if no active agent is available
     */
    HDEmployee assignAgent(HDTicket ticket);
}
