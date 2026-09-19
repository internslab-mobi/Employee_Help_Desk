package com.example.helpdesk.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentProposalResponse {
    
    private Long ticketId;
    private String ticketNumber;
    private Long proposedAgentId;
    private Long employeeId;
    private String employeeName;
    private List<String> matchedSkills;
    private List<String> requiredSkills;
    private Double skillScore;
    private Integer currentWorkload;
    private String status;
}
