package com.divya.helpdesk.service.impl;

import com.divya.helpdesk.entity.*;
import com.divya.helpdesk.enums.EmployeeRole;
import com.divya.helpdesk.enums.EmploymentStatus;
import com.divya.helpdesk.enums.NotificationType;
import com.divya.helpdesk.enums.SlaInstanceStatus;
import com.divya.helpdesk.enums.TicketEventType;
import com.divya.helpdesk.repository.HDEmployeeRepository;
import com.divya.helpdesk.repository.HDSlaInstanceRepository;
import com.divya.helpdesk.repository.HDTicketRepository;
import com.divya.helpdesk.service.EmailService;
import com.divya.helpdesk.service.SlaCalculationService;
import com.divya.helpdesk.service.SlaInstanceService;
import com.divya.helpdesk.service.NotificationService;
import com.divya.helpdesk.service.TicketHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SlaInstanceServiceImpl implements SlaInstanceService {

    private final HDSlaInstanceRepository slaInstanceRepository;
    private final HDTicketRepository ticketRepository;
    private final HDEmployeeRepository employeeRepository;
    private final SlaCalculationService slaCalculationService;
    private final TicketHistoryService ticketHistoryService;
    private final EmailService emailService;
    private final NotificationService notificationService;

    @Override
    public HDSlaInstanceEntity createSlaInstance(HDTicketEntity ticket, HDSlaPolicyEntity policy) {
        Instant startTime = ticket.getWorkStartedAt() != null ? ticket.getWorkStartedAt() : Instant.now();
        Instant deadline = slaCalculationService.calculateDeadline(policy, startTime);
        Instant warningAt = slaCalculationService.calculateWarningTime(policy, startTime);

        // Check if an existing instance exists for this ticket
        HDSlaInstanceEntity instance = slaInstanceRepository.findByTicketId(ticket.getId())
                .orElseGet(HDSlaInstanceEntity::new);

        instance.setTicket(ticket);
        instance.setSlaPolicy(policy);
        instance.setStatus(SlaInstanceStatus.IN_PROGRESS);
        instance.setAllocatedMinutes(policy.getResolutionTimeMinutes());
        instance.setStartedAt(startTime);
        instance.setOriginalDeadlineAt(deadline);
        instance.setCurrentDeadlineAt(deadline);
        instance.setWarningAt(warningAt);
        instance.setWarningSentAt(null);
        instance.setBreachedAt(null);
        instance.setBreachSentAt(null);
        instance.setCycleNumber(ticket.getReopenCount() != null ? ticket.getReopenCount() + 1 : 1);

        log.info("SLA instance initialized for ticket {} with deadline {} and warning threshold {}",
                ticket.getTicketNumber(), deadline, warningAt);

        return slaInstanceRepository.save(instance);
    }

    @Override
    public void resolveSlaInstance(HDTicketEntity ticket) {
        if (ticket == null || ticket.getId() == null) {
            return;
        }
        slaInstanceRepository.findByTicketId(ticket.getId())
                .ifPresent(instance -> {
                    instance.setStatus(SlaInstanceStatus.RESOLVED);
                    slaInstanceRepository.save(instance);
                    log.info("SLA instance resolved for ticket {}", ticket.getTicketNumber());
                });
    }

    @Override
    public void pauseSlaInstance(HDTicketEntity ticket) {
        if (ticket == null || ticket.getId() == null) {
            return;
        }

        slaInstanceRepository.findByTicketId(ticket.getId()).ifPresent(instance -> {
            // Idempotent: Only pause if currently IN_PROGRESS
            if (instance.getStatus() == SlaInstanceStatus.IN_PROGRESS) {
                instance.setStatus(SlaInstanceStatus.PAUSED);
                slaInstanceRepository.save(instance);
                log.info("SLA instance paused for ticket {}", ticket.getTicketNumber());
            }
        });
    }

    @Override
    public void resumeSlaInstance(HDTicketEntity ticket) {
        if (ticket == null || ticket.getId() == null) {
            return;
        }

        slaInstanceRepository.findByTicketId(ticket.getId()).ifPresent(instance -> {
            // Idempotent: Only resume if currently PAUSED
            if (instance.getStatus() == SlaInstanceStatus.PAUSED) {
                Instant pauseStart = ticket.getHoldStartedAt();
                Instant now = Instant.now();

                if (pauseStart != null && pauseStart.isBefore(now)) {
                    HDSlaPolicyEntity policy = instance.getSlaPolicy();
                    Long calendarId = (policy != null && policy.getCalendar() != null)
                            ? policy.getCalendar().getId()
                            : null;

                    int pausedWorkingMinutes;
                    if (calendarId != null) {
                        try {
                            pausedWorkingMinutes = slaCalculationService.calculateWorkingMinutes(calendarId, pauseStart, now);
                        } catch (Exception e) {
                            log.warn("Error calculating working minutes for calendar ID {}: {}", calendarId, e.getMessage());
                            pausedWorkingMinutes = (int) Duration.between(pauseStart, now).toMinutes();
                        }
                    } else {
                        pausedWorkingMinutes = (int) Duration.between(pauseStart, now).toMinutes();
                    }

                    if (pausedWorkingMinutes > 0) {
                        if (calendarId != null) {
                            if (instance.getCurrentDeadlineAt() != null) {
                                instance.setCurrentDeadlineAt(
                                        slaCalculationService.addWorkingMinutes(calendarId, instance.getCurrentDeadlineAt(), pausedWorkingMinutes)
                                );
                            }
                            if (instance.getWarningAt() != null) {
                                instance.setWarningAt(
                                        slaCalculationService.addWorkingMinutes(calendarId, instance.getWarningAt(), pausedWorkingMinutes)
                                );
                            }
                        } else {
                            if (instance.getCurrentDeadlineAt() != null) {
                                instance.setCurrentDeadlineAt(
                                        instance.getCurrentDeadlineAt().plus(Duration.ofMinutes(pausedWorkingMinutes))
                                );
                            }
                            if (instance.getWarningAt() != null) {
                                instance.setWarningAt(
                                        instance.getWarningAt().plus(Duration.ofMinutes(pausedWorkingMinutes))
                                );
                            }
                        }
                    }
                }

                instance.setStatus(SlaInstanceStatus.IN_PROGRESS);
                slaInstanceRepository.save(instance);
                log.info("SLA instance resumed for ticket {} with updated deadline {}",
                        ticket.getTicketNumber(), instance.getCurrentDeadlineAt());
            }
        });
    }

    @Override
    public HDSlaInstanceEntity reopenSlaInstance(HDTicketEntity ticket, HDSlaPolicyEntity policy) {
        Instant startTime = ticket.getWorkStartedAt() != null ? ticket.getWorkStartedAt() : Instant.now();
        int cycleNumber = ticket.getReopenCount() != null ? ticket.getReopenCount() + 1 : 2;
        Instant newDeadline = slaCalculationService.calculateReopenDeadline(policy, cycleNumber, startTime);

        int reopenMinutes = Math.max(1, policy.getResolutionTimeMinutes() / 2);
        int warningMinutes = Math.max(1, (int) Math.round(reopenMinutes * 0.75));
        Instant newWarningAt = slaCalculationService.addWorkingMinutes(policy.getCalendar().getId(), startTime, warningMinutes);

        HDSlaInstanceEntity instance = slaInstanceRepository.findByTicketId(ticket.getId())
                .orElseGet(HDSlaInstanceEntity::new);

        instance.setTicket(ticket);
        instance.setSlaPolicy(policy);
        instance.setStatus(SlaInstanceStatus.IN_PROGRESS);
        instance.setAllocatedMinutes(reopenMinutes);
        instance.setStartedAt(startTime);
        instance.setCurrentDeadlineAt(newDeadline);
        instance.setWarningAt(newWarningAt);
        instance.setWarningSentAt(null);
        instance.setBreachedAt(null);
        instance.setBreachSentAt(null);
        instance.setCycleNumber(cycleNumber);

        log.info("SLA instance reopened (cycle {}) for ticket {} with deadline {}",
                cycleNumber, ticket.getTicketNumber(), newDeadline);

        return slaInstanceRepository.save(instance);
    }

    @Override
    public void checkSlaInstances() {
        Instant now = Instant.now();
        List<HDSlaInstanceEntity> activeInstances = slaInstanceRepository.findByStatus(SlaInstanceStatus.IN_PROGRESS);

        for (HDSlaInstanceEntity instance : activeInstances) {
            HDTicketEntity ticket = instance.getTicket();
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

    private void escalateBreachedTicket(HDTicketEntity ticket, Instant breachTime) {
        HDEmployeeEntity manager = findEscalationManager(ticket);

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

    private HDEmployeeEntity findEscalationManager(HDTicketEntity ticket) {
        if (ticket.getDepartment() == null) {
            return null;
        }
        Long departmentId = ticket.getDepartment().getId();

        // Find active manager in the ticket's department
        List<HDEmployeeEntity> managers = employeeRepository.findByDepartmentIdAndRoleAndEmploymentStatusAndEnabledTrue(
                departmentId,
                EmployeeRole.MANAGER,
                EmploymentStatus.ACTIVE
        );

        if (!managers.isEmpty()) {
            return managers.get(0);
        }

        // Fallback: any active manager in system
        List<HDEmployeeEntity> allManagers = employeeRepository.findByRoleAndEmploymentStatusAndEnabledTrue(
                EmployeeRole.MANAGER,
                EmploymentStatus.ACTIVE
        );

        return allManagers.isEmpty() ? null : allManagers.get(0);
    }

}
