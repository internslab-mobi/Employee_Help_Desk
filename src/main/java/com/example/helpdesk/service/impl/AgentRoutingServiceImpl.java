package com.example.helpdesk.service.impl;

import com.example.helpdesk.dto.response.AssignmentProposalResponse;
import com.example.helpdesk.entity.AgentSkill;
import com.example.helpdesk.entity.DepartmentAgent;
import com.example.helpdesk.entity.Employee;
import com.example.helpdesk.entity.Skill;
import com.example.helpdesk.entity.SubCategorySkill;
import com.example.helpdesk.entity.Ticket;
import com.example.helpdesk.enums.NotificationType;
import com.example.helpdesk.enums.TicketEventType;
import com.example.helpdesk.repository.AgentSkillRepository;
import com.example.helpdesk.repository.DepartmentAgentRepository;
import com.example.helpdesk.repository.SubCategorySkillRepository;
import com.example.helpdesk.repository.TicketRepository;
import com.example.helpdesk.service.AgentRoutingService;
import com.example.helpdesk.service.NotificationService;
import com.example.helpdesk.service.SlaService;
import com.example.helpdesk.service.TicketHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentRoutingServiceImpl implements AgentRoutingService {

    private final TicketRepository ticketRepository;
    private final DepartmentAgentRepository departmentAgentRepository;
    private final SubCategorySkillRepository subCategorySkillRepository;
    private final AgentSkillRepository agentSkillRepository;
    private final TicketHistoryService ticketHistoryService;
    private final SlaService slaService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public AssignmentProposalResponse getAssignmentProposal(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        if (ticket.getAssignedAgent() != null) {
            throw new IllegalStateException("Ticket is already assigned to an agent");
        }

        List<SubCategorySkill> requiredSkills = subCategorySkillRepository.findBySubCategoryId(
                ticket.getSubCategory().getId()
        );

        if (requiredSkills.isEmpty()) {
            throw new IllegalStateException("No required skills configured for sub-category: " 
                    + ticket.getSubCategory().getId());
        }

        List<Long> requiredSkillIds = requiredSkills.stream()
                .map(skill -> skill.getSkill().getId())
                .collect(Collectors.toList());

        List<DepartmentAgent> departmentAgents = departmentAgentRepository.findByDepartmentId(
                ticket.getDepartment().getId()
        );

        if (departmentAgents.isEmpty()) {
            throw new IllegalStateException("No agents found in department: " + ticket.getDepartment().getId());
        }

        List<AgentScore> scoredAgents = new ArrayList<>();

        for (DepartmentAgent agent : departmentAgents) {
            List<AgentSkill> agentSkills = agentSkillRepository.findByAgentId(agent.getId());
            List<Long> agentSkillIds = agentSkills.stream()
                    .map(skill -> skill.getSkill().getId())
                    .collect(Collectors.toList());

            long matchedSkills = requiredSkillIds.stream()
                    .filter(agentSkillIds::contains)
                    .count();

            if (matchedSkills > 0) {
                double skillScore = (matchedSkills * 100.0) / requiredSkillIds.size();
                int workload = calculateWorkload(agent.getId());

                AgentScore agentScore = AgentScore.builder()
                        .agent(agent)
                        .matchedSkills(matchedSkills)
                        .totalRequiredSkills(requiredSkillIds.size())
                        .skillScore(skillScore)
                        .workload(workload)
                        .lastAssignedAt(agent.getLastAssignedAt())
                        .build();

                scoredAgents.add(agentScore);
            }
        }

        if (scoredAgents.isEmpty()) {
            throw new IllegalStateException("No suitable agents found with required skills");
        }

        AgentScore bestAgent = scoredAgents.stream()
                .sorted(Comparator
                        .comparing(AgentScore::getSkillScore).reversed()
                        .thenComparing(AgentScore::getWorkload)
                        .thenComparing(AgentScore::getLastAssignedAt, 
                                Comparator.nullsFirst(Comparator.naturalOrder())))
                .findFirst()
                .orElseThrow();

        List<String> matchedSkillNames = getSkillNames(bestAgent.getAgent().getId(), requiredSkillIds);
        List<String> requiredSkillNames = requiredSkills.stream()
                .map(skill -> skill.getSkill().getName())
                .collect(Collectors.toList());

        ticketHistoryService.recordHistory(
                ticket,
                null,
                TicketEventType.ROUTING_PROPOSED,
                null,
                "Agent " + bestAgent.getAgent().getEmployee().getId() + " proposed",
                Map.of("agentId", bestAgent.getAgent().getId(), "skillScore", bestAgent.getSkillScore())
        );

        return AssignmentProposalResponse.builder()
                .ticketId(ticket.getId())
                .ticketNumber(ticket.getTicketNumber())
                .proposedAgentId(bestAgent.getAgent().getId())
                .employeeId(bestAgent.getAgent().getEmployee().getId())
                .employeeName(bestAgent.getAgent().getEmployee().getFirstName() + " " 
                        + bestAgent.getAgent().getEmployee().getLastName())
                .matchedSkills(matchedSkillNames)
                .requiredSkills(requiredSkillNames)
                .skillScore(bestAgent.getSkillScore())
                .currentWorkload(bestAgent.getWorkload())
                .status("PROPOSED")
                .build();
    }

    @Override
    @Transactional
    public void confirmAssignment(Long ticketId, Long agentId, Boolean confirmed) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        if (ticket.getAssignedAgent() != null) {
            throw new IllegalStateException("Ticket is already assigned");
        }

        DepartmentAgent agent = departmentAgentRepository.findById(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + agentId));

        if (!agent.getDepartment().getId().equals(ticket.getDepartment().getId())) {
            throw new IllegalStateException("Agent does not belong to the ticket's department");
        }

        if (confirmed) {
            List<SubCategorySkill> requiredSkills = subCategorySkillRepository.findBySubCategoryId(
                    ticket.getSubCategory().getId()
            );
            List<Long> requiredSkillIds = requiredSkills.stream()
                    .map(skill -> skill.getSkill().getId())
                    .collect(Collectors.toList());

            List<AgentSkill> agentSkills = agentSkillRepository.findByAgentId(agentId);
            List<Long> agentSkillIds = agentSkills.stream()
                    .map(skill -> skill.getSkill().getId())
                    .collect(Collectors.toList());

            long matchedSkills = requiredSkillIds.stream()
                    .filter(agentSkillIds::contains)
                    .count();

            if (matchedSkills == 0) {
                throw new IllegalStateException("Agent does not have required skills");
            }

            ticket.setAssignedAgent(agent);
            agent.setLastAssignedAt(LocalDateTime.now());
            ticketRepository.save(ticket);
            departmentAgentRepository.save(agent);

            ticketHistoryService.recordHistory(
                    ticket,
                    null,
                    TicketEventType.ASSIGNMENT_CONFIRMED,
                    null,
                    "Agent " + agentId + " confirmed assignment",
                    Map.of("agentId", agentId)
            );

            slaService.createSlaInstance(ticket);

            notificationService.sendNotification(
                    ticket.getRequester(),
                    ticket,
                    NotificationType.ASSIGNMENT_CONFIRMED,
                    "Ticket Assigned",
                    "Your ticket " + ticket.getTicketNumber() + " has been assigned to an agent."
            );

            log.info("Assignment confirmed for ticket {} by agent {}", ticketId, agentId);
        } else {
            ticketHistoryService.recordHistory(
                    ticket,
                    null,
                    TicketEventType.ASSIGNMENT_REJECTED,
                    null,
                    "Agent " + agentId + " rejected assignment",
                    Map.of("agentId", agentId)
            );

            log.info("Assignment rejected for ticket {} by agent {}", ticketId, agentId);
        }
    }

    private int calculateWorkload(Long agentId) {
        List<String> inactiveStatuses = List.of("CLOSED", "RESOLVED", "WITHDRAWN");
        return (int) ticketRepository.countByAssignedAgentIdAndStatusNotIn(agentId, inactiveStatuses);
    }

    private List<String> getSkillNames(Long agentId, List<Long> requiredSkillIds) {
        List<AgentSkill> agentSkills = agentSkillRepository.findByAgentId(agentId);
        return agentSkills.stream()
                .map(AgentSkill::getSkill)
                .filter(skill -> requiredSkillIds.contains(skill.getId()))
                .map(Skill::getName)
                .collect(Collectors.toList());
    }

    @lombok.Data
    @lombok.Builder
    private static class AgentScore {
        private DepartmentAgent agent;
        private long matchedSkills;
        private int totalRequiredSkills;
        private double skillScore;
        private int workload;
        private LocalDateTime lastAssignedAt;
    }
}
