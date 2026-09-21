package com.example.helpdesk.service.impl;

import com.example.helpdesk.dto.request.TicketFeedbackRequest;
import com.example.helpdesk.dto.response.TicketFeedbackResponse;
import com.example.helpdesk.entity.Employee;
import com.example.helpdesk.entity.Ticket;
import com.example.helpdesk.entity.TicketFeedback;
import com.example.helpdesk.enums.NotificationType;
import com.example.helpdesk.enums.TicketEventType;
import com.example.helpdesk.enums.TicketStatus;
import com.example.helpdesk.repository.EmployeeRepository;
import com.example.helpdesk.repository.TicketFeedbackRepository;
import com.example.helpdesk.repository.TicketRepository;
import com.example.helpdesk.service.NotificationService;
import com.example.helpdesk.service.TicketFeedbackService;
import com.example.helpdesk.service.TicketHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TicketFeedbackServiceImpl implements TicketFeedbackService {

    private final TicketFeedbackRepository ticketFeedbackRepository;
    private final TicketRepository ticketRepository;
    private final EmployeeRepository employeeRepository;
    private final TicketHistoryService ticketHistoryService;
    private final NotificationService notificationService;

    @Override
    public TicketFeedbackResponse submitFeedback(Long ticketId, TicketFeedbackRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        Employee submitter = employeeRepository.findById(request.getSubmittedBy())
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + request.getSubmittedBy()));

        if (!ticket.getRequester().getId().equals(submitter.getId())) {
            throw new IllegalStateException("Only the requester can submit feedback");
        }

        if (!ticket.getStatus().equals(TicketStatus.RESOLVED.name()) 
                && !ticket.getStatus().equals(TicketStatus.CLOSED.name())) {
            throw new IllegalStateException("Ticket must be resolved or closed to submit feedback");
        }

        if (ticketFeedbackRepository.findByTicketId(ticketId).isPresent()) {
            throw new IllegalStateException("Feedback already exists for this ticket");
        }

        if (request.getRating() < 1 || request.getRating() > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        TicketFeedback feedback = TicketFeedback.builder()
                .ticket(ticket)
                .submittedBy(submitter)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        feedback = ticketFeedbackRepository.save(feedback);

        ticketHistoryService.recordHistory(
                ticket,
                submitter,
                TicketEventType.FEEDBACK_SUBMITTED,
                null,
                "Feedback submitted with rating: " + request.getRating(),
                null
        );

        if (ticket.getAssignedAgent() != null) {
            notificationService.sendNotification(
                    ticket.getAssignedAgent().getEmployee(),
                    ticket,
                    NotificationType.FEEDBACK_SUBMITTED,
                    "Feedback Received",
                    "Your ticket " + ticket.getTicketNumber() + " received feedback with rating: " + request.getRating()
            );
        }

        log.info("Feedback submitted for ticket {} by {}", ticketId, submitter.getId());

        return toResponse(feedback);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TicketFeedbackResponse> getTicketFeedback(Long ticketId) {
        return ticketFeedbackRepository.findByTicketId(ticketId)
                .map(this::toResponse);
    }

    private TicketFeedbackResponse toResponse(TicketFeedback feedback) {
        return TicketFeedbackResponse.builder()
                .id(feedback.getId())
                .ticketId(feedback.getTicket().getId())
                .submittedById(feedback.getSubmittedBy().getId())
                .submittedByName(feedback.getSubmittedBy().getFirstName() + " " + feedback.getSubmittedBy().getLastName())
                .rating(feedback.getRating())
                .comment(feedback.getComment())
                .createdAt(feedback.getCreatedAt())
                .updatedAt(feedback.getUpdatedAt())
                .build();
    }
}
