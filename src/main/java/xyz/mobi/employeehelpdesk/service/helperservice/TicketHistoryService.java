package xyz.mobi.employeehelpdesk.service.helperservice;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import xyz.mobi.employeehelpdesk.entity.Ticket;
import xyz.mobi.employeehelpdesk.entity.TicketHistory;
import xyz.mobi.employeehelpdesk.entity.enums.HistoryEventType;
import xyz.mobi.employeehelpdesk.entity.enums.TicketStatus;
import xyz.mobi.employeehelpdesk.repository.TicketHistoryRepository;

@Service
@RequiredArgsConstructor
public class TicketHistoryService {

    private final TicketHistoryRepository ticketHistoryRepository;

    public void record(
            Ticket ticket,
            HistoryEventType eventType,
            TicketStatus oldValue,
            TicketStatus newValue
    ) {
        TicketHistory history = new TicketHistory();

        history.setTicket(ticket);
        history.setEventType(eventType);
        history.setOldValue(oldValue);
        history.setNewValue(newValue);

        ticketHistoryRepository.save(history);
    }
}