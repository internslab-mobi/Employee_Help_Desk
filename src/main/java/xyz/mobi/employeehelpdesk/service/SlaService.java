package xyz.mobi.employeehelpdesk.service;

import xyz.mobi.employeehelpdesk.entity.SlaInstance;
import xyz.mobi.employeehelpdesk.entity.Ticket;

public interface SlaService {
    void startSla(Ticket ticket);
    void startReopenSla(Ticket ticket);
    SlaInstance pauseSla(Ticket ticket);
    SlaInstance resumeSla(Ticket ticket);
    SlaInstance completeSla(Ticket ticket);
}
