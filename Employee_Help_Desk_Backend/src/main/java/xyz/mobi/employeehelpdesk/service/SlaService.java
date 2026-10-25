package xyz.mobi.employeehelpdesk.service;

import xyz.mobi.employeehelpdesk.dto.slapolicy.SlaPolicyResponseDTO;
import xyz.mobi.employeehelpdesk.dto.slapolicy.CreateSlaPolicyRequestDTO;
import xyz.mobi.employeehelpdesk.entity.SlaInstance;
import xyz.mobi.employeehelpdesk.entity.Ticket;

import java.util.List;

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
    List<SlaPolicyResponseDTO> getSlaPolicies(Long departmentId);
    SlaPolicyResponseDTO createSlaPolicy(CreateSlaPolicyRequestDTO request);
}
