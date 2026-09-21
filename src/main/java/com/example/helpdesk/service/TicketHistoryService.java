package com.example.helpdesk.service;

import com.example.helpdesk.entity.Employee;
import com.example.helpdesk.entity.Ticket;
import com.example.helpdesk.enums.TicketEventType;

import java.util.Map;

public interface TicketHistoryService {

    void recordHistory(
            Ticket ticket,
            Employee actor,
            TicketEventType eventType,
            String oldValue,
            String newValue,
            Map<String, Object> metadata
    );
}
