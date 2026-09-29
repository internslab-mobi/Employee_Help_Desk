package com.divya.helpdesk.service.impl;

import com.divya.helpdesk.entity.HDEmployee;
import com.divya.helpdesk.entity.HDTicket;
import com.divya.helpdesk.entity.HDTicketHistory;
import com.divya.helpdesk.enums.EmployeeRole;
import com.divya.helpdesk.enums.EmploymentStatus;
import com.divya.helpdesk.enums.TicketEventType;
import com.divya.helpdesk.enums.TicketPriority;
import com.divya.helpdesk.enums.TicketStatus;
import com.divya.helpdesk.repository.HDEmployeeRepository;
import com.divya.helpdesk.repository.HDEmployeeSkillRepository;
import com.divya.helpdesk.repository.HDSubCategorySkillRepository;
import com.divya.helpdesk.repository.HDTicketHistoryRepository;
import com.divya.helpdesk.repository.HDTicketRepository;
import com.divya.helpdesk.service.HDTicketAssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HDTicketAssignmentServiceImpl implements HDTicketAssignmentService {

    private static final List<TicketStatus> ACTIVE_WORKLOAD_STATUSES = List.of(
            TicketStatus.NEW,
            TicketStatus.IN_PROGRESS,
            TicketStatus.WAITING_FOR_EMPLOYEE,
            TicketStatus.REOPENED
    );

    private final HDEmployeeRepository employeeRepository;
    private final HDEmployeeSkillRepository employeeSkillRepository;
    private final HDSubCategorySkillRepository subCategorySkillRepository;
    private final HDTicketRepository ticketRepository;
    private final HDTicketHistoryRepository historyRepository;

    @Override
    public HDEmployee assignAgent(HDTicket ticket) {
        if (ticket == null || ticket.getDepartment() == null) {
            log.warn("Cannot assign agent: ticket or department is null");
            return null;
        }

        Long departmentId = ticket.getDepartment().getId();

        // 1. Find all active agents in ticket's department
        List<HDEmployee> departmentAgents = employeeRepository
                .findByDepartment_IdAndRoleAndEmploymentStatusAndEnabledTrue(departmentId,
                        EmployeeRole.AGENT, EmploymentStatus.ACTIVE);

        if (departmentAgents.isEmpty()) {
            log.warn("No active agents found in department ID: {}. Ticket assignment left unassigned.", departmentId);
            return null;
        }

        // 2. Fetch required skills for the ticket's sub-category
        List<HDEmployee> candidates;
        if (ticket.getSubCategory() != null) {
            Long subCategoryId = ticket.getSubCategory().getId();
            Set<Long> requiredSkillIds = subCategorySkillRepository.findBySubCategory_Id(subCategoryId)
                    .stream()
                    .map(subCategorySkill -> subCategorySkill.getSkill().getId())
                    .collect(Collectors.toSet());

            if (!requiredSkillIds.isEmpty()) {
                // 3. Prefer skill-matching agents
                List<HDEmployee> skilledAgents = departmentAgents.stream()
                        .filter(agent -> hasMatchingSkill(agent.getId(), requiredSkillIds))
                        .toList();

                if (!skilledAgents.isEmpty()) {
                    candidates = skilledAgents;
                    log.info("Found {} skill-matching agents for sub-category ID: {}", candidates.size(), subCategoryId);
                } else {
                    // Fallback to active department agents
                    candidates = departmentAgents;
                    log.info("No skill-matching agents found. Falling back to {} department agents.", candidates.size());
                }
            } else {
                candidates = departmentAgents;
            }
        } else {
            candidates = departmentAgents;
        }

        // 4, 5, 6, 7. Rank candidates by workload, last assignment age, and ID
        return candidates.stream()
                .map(agent -> new AgentWorkload(agent, calculateWorkload(agent.getId()), getLastAssignedTime(agent.getId())))
                .min(Comparator.comparingInt(AgentWorkload::workload)
                                .thenComparing(AgentWorkload::lastAssignedTime, Comparator.nullsFirst(Comparator.naturalOrder()))
                                .thenComparing(aw -> aw.agent().getId())
                )
                .map(AgentWorkload::agent)
                .orElse(null);
    }

    private boolean hasMatchingSkill(Long employeeId, Set<Long> requiredSkillIds) {
        return employeeSkillRepository.findByEmployee_Id(employeeId)
                .stream()
                .anyMatch(es -> requiredSkillIds.contains(es.getSkill().getId()));
    }

    private int calculateWorkload(Long agentId) {
        return ticketRepository.findByAssignedAgent_IdAndStatusIn(agentId, ACTIVE_WORKLOAD_STATUSES)
                .stream()
                .mapToInt(t -> getPriorityWeight(t.getPriority()))
                .sum();
    }

    private int getPriorityWeight(TicketPriority priority) {
        if (priority == null) {
            return 1;
        }
        return switch (priority) {
            case LOW -> 1;
            case MEDIUM -> 2;
            case HIGH -> 3;
            case CRITICAL -> 4;
        };
    }

    private Instant getLastAssignedTime(Long agentId) {
        // Query history for the most recent assignment event
        return historyRepository
                .findTopByNewValueAndEventTypeOrderByCreatedAtDesc(agentId.toString(), TicketEventType.ASSIGNED)
                .map(HDTicketHistory::getCreatedAt)
                .or(() -> ticketRepository.findTopByAssignedAgent_IdOrderByCreatedAtDesc(agentId)
                        .map(HDTicket::getCreatedAt))
                .orElse(null);
    }

    private record AgentWorkload(HDEmployee agent, int workload, Instant lastAssignedTime) {
    }
}
