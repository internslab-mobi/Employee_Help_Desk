package xyz.mobi.employeehelpdesk.service;

import xyz.mobi.employeehelpdesk.dto.slapolicy.SlaPolicyResponseDTO;
import xyz.mobi.employeehelpdesk.entity.SlaInstance;
import xyz.mobi.employeehelpdesk.entity.Ticket;

public interface SlaService {
    SlaInstance startSla(Ticket ticket);
    SlaInstance startReopenSla(Ticket ticket);
    SlaInstance pauseSla(Ticket ticket);
    SlaInstance resumeSla(Ticket ticket);
    SlaInstance completeSla(Ticket ticket);


    SlaPolicyResponseDTO getSlaPolicyById(Long id);


    //evaluation
    void processSlaBreaches();
    void processSlaWarnings();
    void evaluateBreach(Long slaInstanceId);
    void evaluateWarning(Long slaInstanceId);
}
