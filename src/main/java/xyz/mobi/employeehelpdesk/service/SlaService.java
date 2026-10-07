package xyz.mobi.employeehelpdesk.service;

import xyz.mobi.employeehelpdesk.dto.slapolicy.SlaPolicyResponseDTO;
import xyz.mobi.employeehelpdesk.entity.SlaInstance;
import xyz.mobi.employeehelpdesk.entity.Ticket;

public interface SlaService {
    SlaInstance startSla(Ticket ticket);
    SlaInstance startReopenSla(Ticket ticket);
    SlaInstance startBreachRecoverySla(Ticket ticket);
    SlaInstance pauseSla(Ticket ticket);
    SlaInstance resumeSla(Ticket ticket);
    SlaInstance completeSla(Ticket ticket);

    SlaPolicyResponseDTO getSlaPolicyById(Long id);

    // Event-driven SLA processing
    void processSlaEvent(Long slaInstanceId);
}
