package com.divya.helpdesk.service.impl;

import com.divya.helpdesk.dto.ticket.TicketHistoryResponse;
import com.divya.helpdesk.entity.HDEmployee;
import com.divya.helpdesk.entity.HDTicket;
import com.divya.helpdesk.entity.HDTicketHistory;
import com.divya.helpdesk.enums.TicketEventType;
import com.divya.helpdesk.mapper.TicketMapper;
import com.divya.helpdesk.repository.HDTicketHistoryRepository;
import com.divya.helpdesk.service.TicketHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TicketHistoryServiceImpl implements TicketHistoryService {

    private final HDTicketHistoryRepository historyRepository;

    @Override
    public void log(HDTicket ticket, HDEmployee actor, TicketEventType eventType, String oldValue, String newValue) {
        if (ticket == null) {
            log.warn("Cannot log ticket history for null ticket");
            return;
        }

        HDTicketHistory history = new HDTicketHistory();
        history.setTicket(ticket);
        history.setActor(actor);
        history.setEventType(eventType);
        history.setOldValue(oldValue);
        history.setNewValue(newValue);

        historyRepository.save(history);
        log.info("Logged ticket event [{}] for ticket {}", eventType, ticket.getTicketNumber());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketHistoryResponse> getHistory(Long ticketId) {
        return historyRepository.findByTicket_IdOrderByCreatedAtAsc(ticketId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    private TicketHistoryResponse mapToResponse(HDTicketHistory history) {
        return TicketHistoryResponse.builder()
                .id(history.getId())
                .ticketId(history.getTicket() != null ? history.getTicket().getId() : null)
                .actor(TicketMapper.toEmployeeResponse(history.getActor()))
                .eventType(history.getEventType())
                .oldValue(history.getOldValue())
                .newValue(history.getNewValue())
                .createdAt(history.getCreatedAt().atZone(ZoneId.of(history.getActor().getTimezone())).toOffsetDateTime())
                .build();
    }
}