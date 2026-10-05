package xyz.mobi.employeehelpdesk.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketRoutingServiceImpl implements TicketRoutingService {

    private final DepartmentAgentRepository departmentAgentRepository;
    private final TicketRepository ticketRepository;
    private final RoutingStrategy routingStrategy;
    private final TicketHistoryService ticketHistoryService;
    private final NotificationService notificationService;
    private final SubCategorySkillRepository subCategorySkillRepository;
    private final AgentSkillRepository agentSkillRepository;
    private final DepartmentManagerRepository departmentManagerRepository;

    @Override
    @Transactional
    public void routeTicket(Ticket ticket) {

        // Don't overwrite an existing assignment
        if (ticket.getAssignedAgent() != null) {
            return;
        }

        log.debug("Routing ticketId={}, ticketNumber={}, departmentId={}",
                ticket.getId(), ticket.getTicketNumber(), ticket.getDepartment() != null ? ticket.getDepartment().getId() : null);

        // Find eligible agents in the ticket's department
        List<DepartmentAgent> eligibleAgents =
                departmentAgentRepository.findEligibleAgents(
                        ticket.getDepartment().getId()
                );

        // Nobody available -> leave ticket OPEN and unassigned
        // then it should route and notify to department manager
        if (eligibleAgents.isEmpty()) {
            log.info("No eligible agents available in departmentId={} for ticketId={}; ticket left unassigned",
                    ticket.getDepartment().getId(), ticket.getId());
            return;
        }

        // Build candidates
        List<RoutingCandidate> candidates =
                buildCandidates(ticket, eligibleAgents);

        log.debug("Built {} candidate(s) for routing ticketId={}", candidates.size(), ticket.getId());

        // Select the best candidate using routing strategy
        RoutingCandidate selectedCandidate =
                routingStrategy.selectCandidate(candidates);

        log.debug("Selected agentId={} for routing ticketId={}", selectedCandidate.getAgent().getId(), ticket.getId());

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
        ticket.setAssignedManager(resolveDepartmentManager(agent));
        agent.setLastAssignedAt(now);

        ticket.setAssignedAt(now);

        ticketRepository.save(ticket);

        ticketHistoryService.record(ticket, HistoryEventType.ASSIGNED, TicketStatus.OPEN, TicketStatus.OPEN);

        log.info("Ticket routed and assigned: ticketId={}, ticketNumber={}, agentId={}, managerId={}",
                ticket.getId(), ticket.getTicketNumber(), agent.getId(),
                ticket.getAssignedManager() != null ? ticket.getAssignedManager().getId() : null);

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

    private DepartmentManager resolveDepartmentManager(DepartmentAgent agent) {
        if (agent == null || agent.getEmployee() == null) {
            return null;
        }

        Employee agentEmployee = agent.getEmployee();
        return agentEmployee.getManager();
    }
}