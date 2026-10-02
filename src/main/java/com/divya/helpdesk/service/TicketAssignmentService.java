package com.divya.helpdesk.service;

import com.divya.helpdesk.entity.HDEmployeeEntity;
import com.divya.helpdesk.entity.HDTicketEntity;

public interface TicketAssignmentService {

    HDEmployeeEntity assignAgent(HDTicketEntity ticket);
}
