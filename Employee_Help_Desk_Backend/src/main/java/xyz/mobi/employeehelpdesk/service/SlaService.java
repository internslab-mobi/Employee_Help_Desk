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

    // Event-driven SLA processing
    void processSlaEvent(Long slaInstanceId);

    SlaPolicyResponseDTO getSlaPolicyById(Long id);
    java.util.List<SlaPolicyResponseDTO> getSlaPolicies(Long departmentId);
    SlaPolicyResponseDTO createSlaPolicy(xyz.mobi.employeehelpdesk.dto.slapolicy.CreateSlaPolicyRequestDTO request);
}
