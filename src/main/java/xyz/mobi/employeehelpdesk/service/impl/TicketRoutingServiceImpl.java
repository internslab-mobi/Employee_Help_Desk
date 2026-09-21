package xyz.mobi.employeehelpdesk.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.mobi.employeehelpdesk.entity.enums.HistoryEventType;
import xyz.mobi.employeehelpdesk.entity.enums.NotificationType;
import xyz.mobi.employeehelpdesk.entity.enums.TicketStatus;
import xyz.mobi.employeehelpdesk.exception.BadRequestException;
import xyz.mobi.employeehelpdesk.exception.ResourceNotFoundException;
import xyz.mobi.employeehelpdesk.entity.DepartmentAgent;
import xyz.mobi.employeehelpdesk.repository.DepartmentAgentRepository;
import xyz.mobi.employeehelpdesk.entity.Employee;
import xyz.mobi.employeehelpdesk.repository.EmployeeRepository;
import xyz.mobi.employeehelpdesk.entity.RoutingCandidate;
import xyz.mobi.employeehelpdesk.service.helperservice.SkillMatchingService;
import xyz.mobi.employeehelpdesk.service.helperservice.SkillMatchResult;
import xyz.mobi.employeehelpdesk.service.NotificationService;
import xyz.mobi.employeehelpdesk.service.SlaService;
import xyz.mobi.employeehelpdesk.service.TicketRoutingService;
import xyz.mobi.employeehelpdesk.service.helperservice.TicketHistoryService;
import xyz.mobi.employeehelpdesk.strategy.routing.RoutingStrategy;
import xyz.mobi.employeehelpdesk.entity.Ticket;
import xyz.mobi.employeehelpdesk.entity.TicketHistory;
import xyz.mobi.employeehelpdesk.repository.TicketHistoryRepository;
import xyz.mobi.employeehelpdesk.repository.TicketRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketRoutingServiceImpl implements TicketRoutingService {

    private final DepartmentAgentRepository departmentAgentRepository;
    private final TicketRepository ticketRepository;
    private final SkillMatchingService skillMatchingService;
    private final RoutingStrategy routingStrategy;
    private final EmployeeRepository employeeRepository;
    private final TicketHistoryService ticketHistoryService;
    private final SlaService slaService;
    private final NotificationService notificationService;

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

    @Override
    @Transactional
    public void reopenTicket(Long ticketId, Long employeeId) {

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Ticket not found with id: " + ticketId
                        ));

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Employee not found with id: " + employeeId
                        ));

        // Only the requester can reopen the ticket
        if (!ticket.getRequester().getId().equals(employee.getId())) {
            throw new AccessDeniedException(
                    "Only the ticket requester can reopen the ticket"
            );
        }

        // Only RESOLVED tickets can be reopened
        if (ticket.getStatus() != TicketStatus.RESOLVED) {
            throw new BadRequestException(
                    "Only resolved tickets can be reopened"
            );
        }

        // Maximum 2 reopen attempts
        if (ticket.getReopenCount() >= 2) {
            throw new BadRequestException(
                    "Ticket can only be reopened twice"
            );
        }

        // A resolved ticket should have an agent
        if (ticket.getAssignedAgent() == null) {
            throw new BadRequestException(
                    "Cannot reopen ticket because no previous agent is assigned"
            );
        }

        int oldReopenCount = ticket.getReopenCount();

        // Increment reopen count
        ticket.setReopenCount(oldReopenCount + 1);

        // Reopen ticket
        ticket.setStatus(TicketStatus.REOPENED);
        ticket.setReopenedAt(LocalDateTime.now());

        // Clear resolution information
        ticket.setResolvedAt(null);
        ticket.setResolutionSummary(null);

        ticketRepository.save(ticket);

        // Record history
        ticketHistoryService.record(
                ticket,
                HistoryEventType.REOPENED,
                TicketStatus.RESOLVED,
                TicketStatus.REOPENED
        );

        // Start new SLA cycle with half-previous-cycle allocation
        slaService.startReopenSla(ticket);

        String ticketNumber = ticket.getTicketNumber() != null ? ticket.getTicketNumber() : ("#" + ticket.getId());
        if (ticket.getAssignedAgent() != null && ticket.getAssignedAgent().getEmployee() != null) {
            notificationService.sendNotification(
                    ticket.getAssignedAgent().getEmployee(),
                    ticket,
                    NotificationType.TICKET_REOPENED,
                    "Ticket Reopened",
                    "Ticket " + ticketNumber + " has been reopened."
            );
        }
    }

    private List<RoutingCandidate> buildCandidates(
            Ticket ticket,
            List<DepartmentAgent> agents) {

        Collection<TicketStatus> activeStatuses =
                List.of(
                        TicketStatus.OPEN,
                        TicketStatus.IN_PROGRESS,
                        TicketStatus.ON_HOLD,
                        TicketStatus.REOPENED
                );

        return agents.stream()
                .map(agent -> {

                    SkillMatchResult skillMatch =
                            getSkillMatch(ticket, agent);

                    long activeTicketCount =
                            ticketRepository.countActiveTicketsForAgent(
                                    agent.getId(),
                                    activeStatuses
                            );

                    return new RoutingCandidate(
                            agent,
                            skillMatch.matchedSkillCount(),
                            skillMatch.requiredSkillCount(),
                            activeTicketCount
                    );
                })
                .toList();
    }

    private SkillMatchResult getSkillMatch(
            Ticket ticket,
            DepartmentAgent agent) {

        // No subcategory -> no required skills
        if (ticket.getSubCategory() == null) {
            return new SkillMatchResult(0, 0);
        }

        return skillMatchingService.calculateMatch(
                agent,
                ticket.getSubCategory().getId()
        );
    }

    private void assignTicket(
            Ticket ticket,
            DepartmentAgent agent) {

        LocalDateTime now = LocalDateTime.now();

        ticket.setAssignedAgent(agent);
        agent.setLastAssignedAt(now);

        ticket.setAssignedAt(now);

        ticketRepository.save(ticket);

        ticketHistoryService.record(ticket,HistoryEventType.ASSIGNED,TicketStatus.OPEN,TicketStatus.OPEN);

        String ticketNumber = ticket.getTicketNumber() != null ? ticket.getTicketNumber() : ("#" + ticket.getId());
        if (agent.getEmployee() != null) {
            notificationService.sendNotification(
                    agent.getEmployee(),
                    ticket,
                    NotificationType.TICKET_ASSIGNED,
                    "Ticket Assigned",
                    "Ticket " + ticketNumber + " has been assigned to you."
            );
        }
    }
}