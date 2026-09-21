package xyz.mobi.employeehelpdesk.service;

import xyz.mobi.employeehelpdesk.dto.feedback.TicketFeedbackRequestDto;
import xyz.mobi.employeehelpdesk.dto.feedback.TicketFeedbackResponseDto;

public interface TicketFeedbackService {

    TicketFeedbackResponseDto createFeedback(Long ticketId, TicketFeedbackRequestDto request);

    TicketFeedbackResponseDto getFeedback(Long ticketId);
}
