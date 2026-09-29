package xyz.mobi.employeehelpdesk.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.mobi.employeehelpdesk.entity.*;
import xyz.mobi.employeehelpdesk.entity.enums.HistoryEventType;
import xyz.mobi.employeehelpdesk.entity.enums.NotificationType;
import xyz.mobi.employeehelpdesk.entity.enums.TicketStatus;
import xyz.mobi.employeehelpdesk.repository.*;
import xyz.mobi.employeehelpdesk.service.NotificationService;
import xyz.mobi.employeehelpdesk.service.TicketRoutingService;
import xyz.mobi.employeehelpdesk.service.helperservice.TicketHistoryService;
import xyz.mobi.employeehelpdesk.strategy.routing.RoutingStrategy;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TicketRoutingServiceImpl implements TicketRoutingService {

    private final DepartmentAgentRepository departmentAgentRepository;
    private final TicketRepository ticketRepository;
    private final RoutingStrategy routingStrategy;
    private final TicketHistoryService ticketHistoryService;
    private final NotificationService notificationService;
    private final DepartmentManagerRepository departmentManagerRepository;
    private final SubCategorySkillRepository subCategorySkillRepository;
    private final AgentSkillRepository agentSkillRepository;

    @Override
    @Transactional
    public void routeTicket(Ticket ticket) {

        // Don't overwrite an existing assignment
        if (ticket.getAssignedAgent() != null) {
            return;
        }

        // Find eligible agents in the ticket's department
        List<DepartmentAgent> eligibleAgents =
                departmentAgentRepository.findEligibleAgents(
                        ticket.getDepartment().getId()
                );

        // Nobody available -> leave ticket OPEN and unassigned
        // then it should route and notify to department manager
        if (eligibleAgents.isEmpty()) {
            return;
        }

        // Build candidates
        List<RoutingCandidate> candidates =
                buildCandidates(ticket, eligibleAgents);

        // Select the best candidate using routing strategy
        RoutingCandidate selectedCandidate =
                routingStrategy.selectCandidate(candidates);

        // Assign ticket
        assignTicket(ticket, selectedCandidate.getAgent());
    }

    @Override
    @Transactional
    public void retryRouting(Ticket ticket) {

        // Only OPEN unassigned tickets can be retried
        if (ticket.getStatus() != TicketStatus.OPEN) {
            return;
        }

        if (ticket.getAssignedAgent() != null) {
            return;
        }

        routeTicket(ticket);
    }

    private List<RoutingCandidate> buildCandidates(
            Ticket ticket,
            List<DepartmentAgent> agents) {

        if (agents == null || agents.isEmpty()) {
            return Collections.emptyList();
        }

        Collection<TicketStatus> activeStatuses =
                List.of(
                        TicketStatus.OPEN,
                        TicketStatus.IN_PROGRESS,
                        TicketStatus.ON_HOLD,
                        TicketStatus.REOPENED
                );

        List<Long> agentIds = agents.stream()
                .map(DepartmentAgent::getId)
                .toList();

        // 1. Fetch required skill IDs for subcategory once
        Set<Long> requiredSkillIds = Collections.emptySet();
        if (ticket.getSubCategory() != null) {
            List<SubCategorySkill> requiredSkills =
                    subCategorySkillRepository.findBySubCategoryId(ticket.getSubCategory().getId());
            requiredSkillIds = requiredSkills.stream()
                    .map(scs -> scs.getSkill().getId())
                    .collect(Collectors.toSet());
        }
        int requiredSkillCount = requiredSkillIds.size();

        // 2. Batch fetch agent skills for all candidate agents
        List<AgentSkill> agentSkills = agentSkillRepository.findByAgentIdIn(agentIds);
        Map<Long, Set<Long>> agentSkillMap = agentSkills.stream()
                .collect(Collectors.groupingBy(
                        as -> as.getAgent().getId(),
                        Collectors.mapping(as -> as.getSkill().getId(), Collectors.toSet())
                ));

        // 3. Batch count active tickets for all candidate agents
        List<Object[]> countRows = ticketRepository.countActiveTicketsForAgents(agentIds, activeStatuses);
        Map<Long, Long> activeCountMap = countRows.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        // 4. Build routing candidates in-memory with zero per-agent DB queries
        final Set<Long> finalRequiredSkillIds = requiredSkillIds;
        return agents.stream()
                .map(agent -> {
                    Set<Long> skills = agentSkillMap.getOrDefault(agent.getId(), Collections.emptySet());
                    int matchCount = 0;
                    if (!finalRequiredSkillIds.isEmpty()) {
                        for (Long requiredId : finalRequiredSkillIds) {
                            if (skills.contains(requiredId)) {
                                matchCount++;
                            }
                        }
                    }
                    long activeTicketCount = activeCountMap.getOrDefault(agent.getId(), 0L);

                    return new RoutingCandidate(
                            agent,
                            matchCount,
                            requiredSkillCount,
                            activeTicketCount
                    );
                })
                .toList();
    }

    private void assignTicket(
            Ticket ticket,
            DepartmentAgent agent) {

        Instant now = Instant.now();

        ticket.setAssignedAgent(agent);
        ticket.setManagerId(resolveManagerId(agent, ticket));
        agent.setLastAssignedAt(now);

        ticket.setAssignedAt(now);

        ticketRepository.save(ticket);

        ticketHistoryService.record(ticket,HistoryEventType.ASSIGNED,TicketStatus.OPEN,TicketStatus.OPEN);

        String ticketNumber = ticket.getTicketNumber() != null ? ticket.getTicketNumber() : ("#" + ticket.getId());
        if (agent.getEmployee() != null) {
            notificationService.sendNotification(
                    agent.getEmployee().getId(),
                    ticket.getId(),
                    NotificationType.TICKET_ASSIGNED,
                    "Ticket Assigned",
                    "Ticket " + ticketNumber + " has been assigned to you."
            );
        }
    }

    private Long resolveManagerId(DepartmentAgent agent, Ticket ticket) {
        if (agent == null) {
            return null;
        }

        Employee agentEmployee = agent.getEmployee();
        if (agentEmployee != null) {
            // 1. Direct manager from agent's Employee record
            if (agentEmployee.getManager() != null) {
                DepartmentManager dm = agentEmployee.getManager();
                if (dm.getEmployee() != null && dm.getEmployee().getId() != null) {
                    return dm.getEmployee().getId();
                }
                if (dm.getId() != null) {
                    Optional<DepartmentManager> fetchedDm = departmentManagerRepository.findById(dm.getId());
                    if (fetchedDm.isPresent() && fetchedDm.get().getEmployee() != null) {
                        return fetchedDm.get().getEmployee().getId();
                    }
                    return dm.getId();
                }
            }

            // 2. Department relationship: primary or active manager of agent's department
            Long departmentId = agentEmployee.getDepartment() != null
                    ? agentEmployee.getDepartment().getId()
                    : (ticket != null && ticket.getDepartment() != null ? ticket.getDepartment().getId() : null);

            if (departmentId != null) {
                Optional<DepartmentManager> primaryManager =
                        departmentManagerRepository.findByDepartmentIdAndIsPrimaryTrue(departmentId);
                if (primaryManager.isPresent() && primaryManager.get().getEmployee() != null) {
                    return primaryManager.get().getEmployee().getId();
                }

                List<DepartmentManager> managers =
                        departmentManagerRepository.findByDepartmentId(departmentId);
                if (!managers.isEmpty() && managers.get(0).getEmployee() != null) {
                    return managers.get(0).getEmployee().getId();
                }
            }
        } else if (ticket != null && ticket.getDepartment() != null && ticket.getDepartment().getId() != null) {
            Long departmentId = ticket.getDepartment().getId();
            Optional<DepartmentManager> primaryManager =
                    departmentManagerRepository.findByDepartmentIdAndIsPrimaryTrue(departmentId);
            if (primaryManager.isPresent() && primaryManager.get().getEmployee() != null) {
                return primaryManager.get().getEmployee().getId();
            }

            List<DepartmentManager> managers =
                    departmentManagerRepository.findByDepartmentId(departmentId);
            if (!managers.isEmpty() && managers.get(0).getEmployee() != null) {
                return managers.get(0).getEmployee().getId();
            }
        }

        return null;
    }
}