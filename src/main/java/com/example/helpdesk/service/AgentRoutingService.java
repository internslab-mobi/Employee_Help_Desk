package com.example.helpdesk.service;

import com.example.helpdesk.dto.response.AssignmentProposalResponse;
import com.example.helpdesk.entity.Ticket;

public interface AgentRoutingService {
    
    AssignmentProposalResponse getAssignmentProposal(Long ticketId);
    
    void confirmAssignment(Long ticketId, Long agentId, Boolean confirmed);
}
