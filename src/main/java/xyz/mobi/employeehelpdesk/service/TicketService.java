package xyz.mobi.employeehelpdesk.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import xyz.mobi.employeehelpdesk.dto.ticket.CreateTicketRequest;
import xyz.mobi.employeehelpdesk.dto.ticket.HoldTicketRequestDto;
import xyz.mobi.employeehelpdesk.dto.ticket.ResolveTicketRequestDto;
import xyz.mobi.employeehelpdesk.dto.ticket.TicketResponse;
import xyz.mobi.employeehelpdesk.dto.ticket.WithdrawRequestDto;
import xyz.mobi.employeehelpdesk.entity.enums.TicketStatus;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface TicketService {

    public TicketResponse createTicket(
            CreateTicketRequest request,
            List<MultipartFile> attachments,
            Long requesterId) throws IOException;

    Map<String,Integer> getEmployeeTicketStatus();

    Map<String,Integer> getAgentTicketStatus();

    public Page<TicketResponse> getMyTickets(
            TicketStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            String search,
            Pageable pageable
    );

    Page<TicketResponse> getAgentTickets(
            TicketStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            String search,
            Pageable pageable
    );

    TicketResponse withdrawTicket(Long ticketId, WithdrawRequestDto withdrawRequest);

    TicketResponse startTicket(Long ticketId);

    TicketResponse holdTicket(Long ticketId, HoldTicketRequestDto request);

    TicketResponse resumeTicket(Long ticketId);

    TicketResponse resolveTicket(Long ticketId, ResolveTicketRequestDto request);
}
