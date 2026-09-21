package com.example.helpdesk.service;

import com.example.helpdesk.dto.request.TicketFeedbackRequest;
import com.example.helpdesk.dto.response.TicketFeedbackResponse;

import java.util.Optional;

public interface TicketFeedbackService {

    TicketFeedbackResponse submitFeedback(Long ticketId, TicketFeedbackRequest request);

    Optional<TicketFeedbackResponse> getTicketFeedback(Long ticketId);
}
