package com.example.helpdesk.service.impl;

import com.example.helpdesk.entity.Employee;
import com.example.helpdesk.entity.Ticket;
import com.example.helpdesk.entity.TicketHistory;
import com.example.helpdesk.enums.TicketEventType;
import com.example.helpdesk.repository.TicketHistoryRepository;
import com.example.helpdesk.service.TicketHistoryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketHistoryServiceImpl implements TicketHistoryService {

    private final TicketHistoryRepository ticketHistoryRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void recordHistory(
            Ticket ticket,
            Employee actor,
            TicketEventType eventType,
            String oldValue,
            String newValue,
            Map<String, Object> metadata
    ) {
        try {
            String metadataJson = metadata != null ? objectMapper.writeValueAsString(metadata) : null;

            TicketHistory history = TicketHistory.builder()
                    .ticket(ticket)
                    .actor(actor)
                    .eventType(eventType != null ? eventType.name() : null)
                    .oldValue(oldValue)
                    .newValue(newValue)
                    .metadata(metadataJson)
                    .build();

            ticketHistoryRepository.save(history);
            log.debug("Recorded history for ticket {}: event={}, actor={}", 
                    ticket.getId(), eventType, actor != null ? actor.getId() : "system");
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize metadata for ticket history", e);
            throw new RuntimeException("Failed to record ticket history", e);
        }
    }
}
