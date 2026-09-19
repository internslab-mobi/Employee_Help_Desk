package com.divya.helpdesk.service.impl;

import com.divya.helpdesk.entity.*;
import com.divya.helpdesk.enums.HDTicketStatus;
import com.divya.helpdesk.repository.*;
import com.divya.helpdesk.service.TicketAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TicketAssignmentServiceImpl implements TicketAssignmentService {

    private final HDDepartmentAgentRepository departmentAgentRepository;
    private final HDDepartmentManagerRepository departmentManagerRepository;
    private final HDAgentSkillRepository agentSkillRepository;
    private final HDSubCategorySkillRepository subCategorySkillRepository;
    private final HDTicketRepository ticketRepository;

    @Override
    public void assignAgentOrEscalate(HDTicket ticket) {
        if (ticket == null || ticket.getDepartment() == null || ticket.getSubCategory() == null) {
            return;
        }

        Long departmentId = ticket.getDepartment().getId();
        Long subCategoryId = ticket.getSubCategory().getId();

        // get all active agents in the department
        List<HDDepartmentAgent> activeAgents = departmentAgentRepository.findActiveAgentsByDepartmentId(departmentId);

        // get required skills for this subcategory
        List<HDSubCategorySkill> subCategorySkills = subCategorySkillRepository.findBySubCategoryId(subCategoryId);
        Set<Long> requiredSkillIds = subCategorySkills.stream()
                .map(scs -> scs.getSkill().getId())
                .collect(Collectors.toSet());

        // filter agents that possess all required skills
        List<HDDepartmentAgent> candidateAgents = new ArrayList<>();
        for (HDDepartmentAgent agent : activeAgents) {
            if (requiredSkillIds.isEmpty()) {
                candidateAgents.add(agent);
            } else {
                List<HDAgentSkill> agentSkills = agentSkillRepository.findByAgentId(agent.getId());
                Set<Long> agentSkillIds = agentSkills.stream()
                        .map(ask -> ask.getSkill().getId())
                        .collect(Collectors.toSet());

                if (agentSkillIds.containsAll(requiredSkillIds)) {
                    candidateAgents.add(agent);
                }
            }
        }

        // if matching agents found, select best agent by workload -> lastAssignedAt -> min empId
        if (!candidateAgents.isEmpty()) {
            HDDepartmentAgent chosenAgent = selectBestAgent(candidateAgents);
            ticket.setAssignedAgent(chosenAgent.getEmployee());
            ticket.setAssignedManager(null);
            ticket.setStatus(HDTicketStatus.ASSIGNED);

            // Update lastAssignedAt timestamp on the agent
            chosenAgent.setLastAssignedAt(LocalDateTime.now());
            departmentAgentRepository.save(chosenAgent);
        } else {
            // escalate to Department Manager if no matching agent exists
            escalateToManager(ticket, departmentId);
        }
    }

    private HDDepartmentAgent selectBestAgent(List<HDDepartmentAgent> candidateAgents) {
        // compute workload for each candidate agent
        Map<HDDepartmentAgent, Integer> agentWorkloads = new HashMap<>();
        for (HDDepartmentAgent agent : candidateAgents) {
            int workload = calculateAgentWorkload(agent.getEmployee().getId());
            agentWorkloads.put(agent, workload);
        }
        // Comparator:
        // 1. Lowest workload
        // 2. Least recently assigned (nulls first, then earliest timestamp)
        // 3. Min employee ID ASC
        Comparator<HDDepartmentAgent> agentComparator = Comparator
                .comparingInt((HDDepartmentAgent a) -> agentWorkloads.get(a))
                .thenComparing(HDDepartmentAgent::getLastAssignedAt,
                        Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(a -> a.getEmployee().getId());

        return candidateAgents.stream()
                .min(agentComparator)
                .orElse(candidateAgents.getFirst());
    }

    @Override
    @Transactional(readOnly = true)
    public int calculateAgentWorkload(Long employeeId) {
        List<HDTicket> activeTickets = ticketRepository.findActiveTicketsByAgentEmployeeId(employeeId);
        return activeTickets.stream()
                .mapToInt(t -> t.getPriority() != null ? t.getPriority().getWeight() : 1)
                .sum();
    }

    private void escalateToManager(HDTicket ticket, Long departmentId) {
        List<HDDepartmentManager> managers = departmentManagerRepository.findManagersByDepartmentIdOrderedByPrimary(departmentId);
        if (!managers.isEmpty()) {
            HDDepartmentManager primaryManager = managers.getFirst();
            ticket.setAssignedManager(primaryManager.getEmployee());
            ticket.setAssignedAgent(null);
            ticket.setStatus(HDTicketStatus.NEW);
        } else {
            ticket.setAssignedManager(null);
            ticket.setAssignedAgent(null);
            ticket.setStatus(HDTicketStatus.NEW);
        }
    }
}
