package xyz.mobi.employeehelpdesk.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.mobi.employeehelpdesk.dto.feedback.TicketFeedbackRequestDto;
import xyz.mobi.employeehelpdesk.dto.feedback.TicketFeedbackResponseDto;
import xyz.mobi.employeehelpdesk.entity.Employee;
import xyz.mobi.employeehelpdesk.entity.Ticket;
import xyz.mobi.employeehelpdesk.entity.TicketFeedback;
import xyz.mobi.employeehelpdesk.entity.enums.HistoryEventType;
import xyz.mobi.employeehelpdesk.entity.enums.NotificationType;
import xyz.mobi.employeehelpdesk.entity.enums.TicketStatus;
import xyz.mobi.employeehelpdesk.exception.BadRequestException;
import xyz.mobi.employeehelpdesk.exception.ResourceNotFoundException;
import xyz.mobi.employeehelpdesk.mapper.TicketFeedbackMapper;
import xyz.mobi.employeehelpdesk.repository.EmployeeRepository;
import xyz.mobi.employeehelpdesk.repository.TicketFeedbackRepository;
import xyz.mobi.employeehelpdesk.repository.TicketRepository;
import xyz.mobi.employeehelpdesk.service.CurrentUserService;
import xyz.mobi.employeehelpdesk.service.NotificationService;
import xyz.mobi.employeehelpdesk.service.TicketFeedbackService;
import xyz.mobi.employeehelpdesk.service.helperservice.TicketHistoryService;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketFeedbackServiceImpl implements TicketFeedbackService {

    private final TicketRepository ticketRepository;
    private final TicketFeedbackRepository ticketFeedbackRepository;
    private final EmployeeRepository employeeRepository;
    private final TicketHistoryService ticketHistoryService;
    private final NotificationService notificationService;
    private final TicketFeedbackMapper ticketFeedbackMapper;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public TicketFeedbackResponseDto createFeedback(Long ticketId, TicketFeedbackRequestDto request) {
        long currentEmployeeId = currentUserService.getCurrentEmployeeId();

        if (request == null || request.rating() == null) {
            throw new BadRequestException("Rating is required");
        }
        if (request.rating() < 1 || request.rating() > 5) {
            throw new BadRequestException("Rating must be between 1 and 5");
        }

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));

        if (!ticket.getRequester().getId().equals(currentEmployeeId)) {
            throw new AccessDeniedException("Only the ticket requester can submit feedback");
        }

        if (ticket.getStatus() != TicketStatus.RESOLVED) {
            throw new BadRequestException("Feedback can only be submitted for resolved tickets");
        }

        if (ticketFeedbackRepository.existsByTicketId(ticketId)) {
            throw new BadRequestException("Feedback has already been submitted for this ticket");
        }

        Employee submitter = employeeRepository.findById(currentEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + currentEmployeeId));

        TicketFeedback feedback = new TicketFeedback();
        feedback.setTicket(ticket);
        feedback.setSubmittedBy(submitter);
        feedback.setRating(request.rating());
        feedback.setComment(request.comment());

        feedback = ticketFeedbackRepository.save(feedback);

        ticketHistoryService.record(
                ticket,
                HistoryEventType.FEEDBACK_SUBMITTED,
                ticket.getStatus(),
                ticket.getStatus()
        );

        String ticketNumber = ticket.getTicketNumber() != null ? ticket.getTicketNumber() : ("#" + ticket.getId());
        if (ticket.getAssignedAgent() != null && ticket.getAssignedAgent().getEmployee() != null) {
            notificationService.sendNotification(
                    ticket.getAssignedAgent().getEmployee(),
                    ticket,
                    NotificationType.FEEDBACK_RECEIVED,
                    "Feedback Received",
                    "Feedback has been submitted for ticket " + ticketNumber + "."
            );
        }

        return ticketFeedbackMapper.toResponse(feedback);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketFeedbackResponseDto getFeedback(Long ticketId) {
        long currentEmployeeId = currentUserService.getCurrentEmployeeId();

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));

        boolean isRequester = ticket.getRequester().getId().equals(currentEmployeeId);
        boolean isAssignedAgent = ticket.getAssignedAgent() != null
                && (ticket.getAssignedAgent().getEmployee().getId().equals(currentEmployeeId)
                || ticket.getAssignedAgent().getId().equals(currentEmployeeId));

        if (!isRequester && !isAssignedAgent) {
            throw new AccessDeniedException("You are not allowed to view feedback for this ticket");
        }

        TicketFeedback feedback = ticketFeedbackRepository.findByTicketId(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback not found for ticket: " + ticketId));

        return ticketFeedbackMapper.toResponse(feedback);
    }
}
