package com.example.helpdesk.scheduler;

import com.example.helpdesk.entity.AgentSkill;
import com.example.helpdesk.entity.DepartmentAgent;
import com.example.helpdesk.entity.SubCategorySkill;
import com.example.helpdesk.entity.Ticket;
import com.example.helpdesk.entity.TicketSla;
import com.example.helpdesk.enums.SlaStatus;
import com.example.helpdesk.enums.TicketEventType;
import com.example.helpdesk.repository.AgentSkillRepository;
import com.example.helpdesk.repository.DepartmentAgentRepository;
import com.example.helpdesk.repository.SubCategorySkillRepository;
import com.example.helpdesk.repository.TicketSlaRepository;
import com.example.helpdesk.service.NotificationService;
import com.example.helpdesk.service.TicketHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SlaEscalationScheduler {

    private final TicketSlaRepository ticketSlaRepository;
    private final DepartmentAgentRepository departmentAgentRepository;
    private final SubCategorySkillRepository subCategorySkillRepository;
    private final AgentSkillRepository agentSkillRepository;
    private final TicketHistoryService ticketHistoryService;
    private final NotificationService notificationService;

    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    @Transactional
    public void checkAndEscalateBreachedSlas() {
        Instant now = Instant.now();

        // Find breached SLAs that haven't been escalated yet
        List<TicketSla> breachedSlas = ticketSlaRepository.findByStatusAndCurrentDeadlineAtBefore(
                SlaStatus.BREACHED.name(),
                now
        );

        for (TicketSla ticketSla : breachedSlas) {
            // Check if this SLA has already been escalated (via history check)
            // For simplicity, we'll use a flag or check if ticket has been reassigned recently
            // Here we'll escalate if the ticket is still assigned to the same agent and hasn't been escalated recently

            Ticket ticket = ticketSla.getTicket();
            if (ticket.getAssignedAgent() == null) {
                continue; // No agent assigned, skip
            }

            // Check if escalation already happened recently (e.g., within last hour)
            // This prevents repeated escalation for the same breach
            // We'll use a simple check: if the ticket was reassigned in the last hour, skip
            if (ticket.getUpdatedAt().isAfter(now.minus(java.time.Duration.ofHours(1)))) {
                continue;
            }

            try {
                escalateTicket(ticket, ticketSla);
            } catch (Exception e) {
                log.error("Failed to escalate ticket {} for SLA breach: {}", ticket.getId(), e.getMessage());
            }
        }
    }

    private void escalateTicket(Ticket ticket, TicketSla ticketSla) {
        Long currentAgentId = ticket.getAssignedAgent().getId();

        // Find replacement agent using existing routing logic
        DepartmentAgent newAgent = findReplacementAgent(ticket, currentAgentId);

        if (newAgent == null) {
            log.warn("No replacement agent found for ticket {} escalation", ticket.getId());
            return;
        }

        // Reassign ticket
        ticket.setAssignedAgent(newAgent);
        newAgent.setLastAssignedAt(Instant.now());

        // Update ticket
        ticket.setUpdatedAt(Instant.now());

        // Record history
        ticketHistoryService.recordHistory(
                ticket,
                ticket.getAssignedAgent().getEmployee(),
                TicketEventType.TICKET_ESCALATED,
                String.valueOf(currentAgentId),
                String.valueOf(newAgent.getId()),
                null
        );

        // Notify new agent
        notificationService.sendNotification(
                newAgent.getEmployee(),
                ticket,
                com.example.helpdesk.enums.NotificationType.TICKET_ESCALATED,
                "Ticket Escalated - Reassigned to You",
                "Ticket " + ticket.getTicketNumber() + " has been escalated due to SLA breach and reassigned to you."
        );

        // Notify manager if assigned
        if (ticket.getAssignedManager() != null) {
            notificationService.sendNotification(
                    ticket.getAssignedManager().getEmployee(),
                    ticket,
                    com.example.helpdesk.enums.NotificationType.TICKET_ESCALATED,
                    "Ticket Escalated",
                    "Ticket " + ticket.getTicketNumber() + " has been escalated and reassigned from agent " + currentAgentId + " to agent " + newAgent.getId() + "."
            );
        }

        log.info("Escalated ticket {} from agent {} to agent {} due to SLA breach",
                ticket.getId(), currentAgentId, newAgent.getId());
    }

    private DepartmentAgent findReplacementAgent(Ticket ticket, Long excludeAgentId) {
        // Get required skills for the ticket's sub-category
        List<SubCategorySkill> requiredSkills = subCategorySkillRepository.findBySubCategoryId(
                ticket.getSubCategory().getId()
        );

        if (requiredSkills.isEmpty()) {
            return null;
        }

        List<Long> requiredSkillIds = requiredSkills.stream()
                .map(skill -> skill.getSkill().getId())
                .toList();

        // Get all agents in the department except the current agent
        List<DepartmentAgent> departmentAgents = departmentAgentRepository
                .findByDepartmentId(ticket.getDepartment().getId())
                .stream()
                .filter(agent -> !agent.getId().equals(excludeAgentId))
                .toList();

        if (departmentAgents.isEmpty()) {
            return null;
        }

        // Score agents based on skills, workload, and last assignment
        var scoredAgents = new java.util.ArrayList<AgentScore>();

        for (DepartmentAgent agent : departmentAgents) {
            List<AgentSkill> agentSkills = agentSkillRepository.findByAgentId(agent.getId());

            List<Long> agentSkillIds = agentSkills.stream()
                    .map(skill -> skill.getSkill().getId())
                    .toList();

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
            return null;
        }

        // Select best agent
        return scoredAgents.stream()
                .sorted(Comparator
                        .comparing(AgentScore::getSkillScore).reversed()
                        .thenComparing(AgentScore::getWorkload)
                        .thenComparing(AgentScore::getLastAssignedAt,
                                Comparator.nullsFirst(Comparator.naturalOrder())))
                .findFirst()
                .map(AgentScore::getAgent)
                .orElse(null);
    }

    private int calculateWorkload(Long agentId) {
        List<String> inactiveStatuses = List.of("CLOSED", "RESOLVED", "WITHDRAWN", "CANCELLED");
        return (int) departmentAgentRepository.countByIdAndTicketStatusNotIn(agentId, inactiveStatuses);
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class AgentScore {
        private DepartmentAgent agent;
        private long matchedSkills;
        private long totalRequiredSkills;
        private double skillScore;
        private int workload;
        private Instant lastAssignedAt;
    }
}
