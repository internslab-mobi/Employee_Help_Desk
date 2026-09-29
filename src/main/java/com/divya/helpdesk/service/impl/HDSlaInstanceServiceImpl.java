package com.divya.helpdesk.service.impl;

import com.divya.helpdesk.entity.HDEmployee;
import com.divya.helpdesk.entity.HDSlaInstance;
import com.divya.helpdesk.entity.HDSlaPolicy;
import com.divya.helpdesk.entity.HDTicket;
import com.divya.helpdesk.enums.EmployeeRole;
import com.divya.helpdesk.enums.EmploymentStatus;
import com.divya.helpdesk.enums.NotificationType;
import com.divya.helpdesk.enums.SlaInstanceStatus;
import com.divya.helpdesk.enums.TicketEventType;
import com.divya.helpdesk.repository.HDEmployeeRepository;
import com.divya.helpdesk.repository.HDSlaInstanceRepository;
import com.divya.helpdesk.repository.HDTicketRepository;
import com.divya.helpdesk.service.EmailService;
import com.divya.helpdesk.service.HDSlaCalculationService;
import com.divya.helpdesk.service.HDSlaInstanceService;
import com.divya.helpdesk.service.NotificationService;
import com.divya.helpdesk.service.TicketHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class HDSlaInstanceServiceImpl implements HDSlaInstanceService {

    private final HDSlaInstanceRepository slaInstanceRepository;
    private final HDTicketRepository ticketRepository;
    private final HDEmployeeRepository employeeRepository;
    private final HDSlaCalculationService slaCalculationService;
    private final TicketHistoryService ticketHistoryService;
    private final EmailService emailService;
    private final NotificationService notificationService;

    @Override
    public HDSlaInstance createSlaInstance(HDTicket ticket, HDSlaPolicy policy) {
        Instant now = Instant.now();
        Instant deadline = slaCalculationService.calculateDeadline(policy, now);
        Instant warningAt = slaCalculationService.calculateWarningTime(policy, now);

        // Check if an existing instance exists for this ticket
        HDSlaInstance instance = slaInstanceRepository.findByTicket_Id(ticket.getId())
                .orElseGet(HDSlaInstance::new);

        instance.setTicket(ticket);
        instance.setSlaPolicy(policy);
        instance.setStatus(SlaInstanceStatus.IN_PROGRESS);
        instance.setAllocatedMinutes(policy.getResolutionTimeMinutes());
        instance.setStartedAt(now);
        instance.setOriginalDeadlineAt(deadline);
        instance.setCurrentDeadlineAt(deadline);
        instance.setWarningAt(warningAt);
        instance.setWarningSentAt(null);
        instance.setBreachedAt(null);
        instance.setBreachSentAt(null);
        instance.setCycleNumber(ticket.getReopenCount() != null ? ticket.getReopenCount() + 1 : 1);

        log.info("SLA instance initialized for ticket {} with deadline {} and warning threshold {}",
                ticket.getTicketNumber(), deadline, warningAt.atZone(ZoneId.of(ticket.getRequester().getTimezone())).toOffsetDateTime());

        return slaInstanceRepository.save(instance);
    }

    @Override
    public void resolveSlaInstance(HDTicket ticket) {
        slaInstanceRepository.findByTicket_Id(ticket.getId())
                .ifPresent(instance -> {
                    instance.setStatus(SlaInstanceStatus.RESOLVED);
                    slaInstanceRepository.save(instance);
                    log.info("SLA instance resolved for ticket {}", ticket.getTicketNumber());
                });
    }

    @Override
    public HDSlaInstance reopenSlaInstance(HDTicket ticket, HDSlaPolicy policy) {
        Instant now = Instant.now();
        int cycleNumber = ticket.getReopenCount() != null ? ticket.getReopenCount() + 1 : 2;
        Instant newDeadline = slaCalculationService.calculateReopenDeadline(policy, cycleNumber, now);

        int reopenMinutes = Math.max(1, policy.getResolutionTimeMinutes() / 2);
        int warningMinutes = Math.max(1, (int) Math.round(reopenMinutes * 0.75));
        Instant newWarningAt = slaCalculationService.addWorkingMinutes(policy.getCalendar().getId(), now, warningMinutes);

        HDSlaInstance instance = slaInstanceRepository.findByTicket_Id(ticket.getId())
                .orElseGet(HDSlaInstance::new);

        instance.setTicket(ticket);
        instance.setSlaPolicy(policy);
        instance.setStatus(SlaInstanceStatus.IN_PROGRESS);
        instance.setAllocatedMinutes(reopenMinutes);
        instance.setStartedAt(now);
        instance.setCurrentDeadlineAt(newDeadline);
        instance.setWarningAt(newWarningAt);
        instance.setWarningSentAt(null);
        instance.setBreachedAt(null);
        instance.setBreachSentAt(null);
        instance.setCycleNumber(cycleNumber);

        log.info("SLA instance reopened (cycle {}) for ticket {} with deadline {}",
                cycleNumber, ticket.getTicketNumber(), newDeadline.atZone(ZoneId.of(ticket.getRequester().getTimezone())).toOffsetDateTime());

        return slaInstanceRepository.save(instance);
    }

    @Override
    public void checkSlaInstances() {
        Instant now = Instant.now();
        List<HDSlaInstance> activeInstances = slaInstanceRepository.findByStatus(SlaInstanceStatus.IN_PROGRESS);

        for (HDSlaInstance instance : activeInstances) {
            HDTicket ticket = instance.getTicket();
            if (ticket == null) {
                continue;
            }
            // 1. Check Warning Threshold
            if (instance.getWarningAt() != null && !now.isBefore(instance.getWarningAt()) && instance.getWarningSentAt() == null) {
                instance.setWarningSentAt(now);
                slaInstanceRepository.save(instance);

                ticketHistoryService.log(ticket, null, TicketEventType.SLA_WARNING,
                        null,
                        "SLA warning threshold reached at " + now);

                emailService.sendSlaWarningEmail(ticket);
                if (ticket.getAssignedAgent() != null) {
                    notificationService.createNotification(ticket.getAssignedAgent(),
                            "SLA Warning: " + ticket.getTicketNumber(),
                            " Ticket " + ticket.getTicketNumber() + " has reached the 75% SLA warning threshold.",
                            NotificationType.SLA_WARNING,
                            ticket
                    );
                }
                log.warn("SLA warning dispatched for ticket {}", ticket.getTicketNumber());
            }

            // 2. Check Breach Threshold & Trigger Manager Escalation
            if (instance.getCurrentDeadlineAt() != null
                    && !now.isBefore(instance.getCurrentDeadlineAt())
                    && instance.getBreachedAt() == null) {

                instance.setBreachedAt(now);
                instance.setBreachSentAt(now);
                instance.setStatus(SlaInstanceStatus.BREACHED);
                slaInstanceRepository.save(instance);

                // Escalate to Manager of assigned agent or department
                escalateBreachedTicket(ticket, now);
            }
        }
    }

    private void escalateBreachedTicket(HDTicket ticket, Instant breachTime) {
        HDEmployee manager = findEscalationManager(ticket);

        if (manager != null) {
            ticket.setAssignedManager(manager);
            ticketRepository.save(ticket);

            ticketHistoryService.log(
                    ticket,
                    null,
                    TicketEventType.SLA_BREACHED,
                    null,
                    "SLA Breached at " + breachTime + ". Escalated to Manager: "
                            + manager.getFirstName() + " " + manager.getLastName() + " (" + manager.getEmail() + ")"
            );

            emailService.sendTicketEscalatedEmail(ticket, manager);
            notificationService.createNotification(
                    manager,
                    "Ticket Escalation: " + ticket.getTicketNumber(),
                    "Ticket " + ticket.getTicketNumber() + " has breached SLA and has been escalated to you.",
                    NotificationType.SLA_BREACHED,
                    ticket
            );
            log.error("Ticket {} breached SLA. Escalated to manager: {}", ticket.getTicketNumber(), manager.getEmail());
        } else {
            ticketHistoryService.log(
                    ticket,
                    null,
                    TicketEventType.SLA_BREACHED,
                    null,
                    "SLA Breached at " + breachTime + " (No manager available for automatic escalation)"
            );
            log.error("Ticket {} breached SLA. No manager available in department for escalation.", ticket.getTicketNumber());
        }

        // Send SLA breached email and notification to agent/requester
        emailService.sendSlaBreachedEmail(ticket);
        if (ticket.getAssignedAgent() != null) {
            notificationService.createNotification(
                    ticket.getAssignedAgent(),
                    "SLA Breached: " + ticket.getTicketNumber(),
                    "Ticket " + ticket.getTicketNumber() + " has breached SLA resolution deadline.",
                    NotificationType.SLA_BREACHED,
                    ticket
            );
        }
    }

    private HDEmployee findEscalationManager(HDTicket ticket) {
        if (ticket.getDepartment() == null) {
            return null;
        }
        Long departmentId = ticket.getDepartment().getId();

        // Find active manager in the ticket's department
        List<HDEmployee> managers = employeeRepository.findByDepartment_IdAndRoleAndEmploymentStatusAndEnabledTrue(
                departmentId,
                EmployeeRole.MANAGER,
                EmploymentStatus.ACTIVE
        );

        if (!managers.isEmpty()) {
            return managers.get(0);
        }

        // Fallback: any active manager in system
        List<HDEmployee> allManagers = employeeRepository.findByRoleAndEmploymentStatusAndEnabledTrue(
                EmployeeRole.MANAGER,
                EmploymentStatus.ACTIVE
        );

        return allManagers.isEmpty() ? null : allManagers.get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBreached(HDTicket ticket) {
        if (ticket == null || ticket.getId() == null) {
            return false;
        }
        return slaInstanceRepository.findByTicket_Id(ticket.getId())
                .map(instance -> instance.getBreachedAt() != null || (instance.getCurrentDeadlineAt() != null && Instant.now().isAfter(instance.getCurrentDeadlineAt())))
                .orElse(false);
    }
}
