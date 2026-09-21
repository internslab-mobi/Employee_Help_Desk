package com.example.helpdesk.service.impl;

import com.example.helpdesk.entity.SlaRule;
import com.example.helpdesk.entity.Ticket;
import com.example.helpdesk.entity.TicketSla;
import com.example.helpdesk.enums.SlaStatus;
import com.example.helpdesk.repository.SlaRuleRepository;
import com.example.helpdesk.repository.TicketRepository;
import com.example.helpdesk.repository.TicketSlaRepository;
import com.example.helpdesk.service.BusinessTimeService;
import com.example.helpdesk.service.SlaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlaServiceImpl implements SlaService {

    private final TicketSlaRepository ticketSlaRepository;
    private final SlaRuleRepository slaRuleRepository;
    private final BusinessTimeService businessTimeService;
    private final TicketRepository ticketRepository;

    @Override
    @Transactional
    public TicketSla createSlaInstance(Ticket ticket) {

        SlaRule slaRule =
                slaRuleRepository.findByDepartmentIdAndSubCategoryIdAndActiveTrue(
                        ticket.getDepartment().getId(),
                        ticket.getSubCategory().getId()
                );

        if (slaRule == null) {
            log.warn(
                    "No active SLA rule found for department {} and sub-category {}",
                    ticket.getDepartment().getId(),
                    ticket.getSubCategory().getId()
            );
            return null;
        }

        // Link the selected SLA policy to the ticket
        ticket.setSlaPolicy(slaRule);
        ticketRepository.save(ticket);

        LocalDateTime slaStart =
                businessTimeService.getNextWorkingTime(LocalDateTime.now());

        LocalDateTime deadline =
                businessTimeService.addWorkingMinutes(
                        slaStart,
                        slaRule.getDurationMinutes()
                );

        LocalDateTime warningTime =
                slaRule.getWarningMinutes() != null
                        ? businessTimeService.addWorkingMinutes(
                        slaStart,
                        slaRule.getDurationMinutes()
                        - slaRule.getWarningMinutes()
                )
                        : null;

        TicketSla ticketSla = TicketSla.builder()
                .ticket(ticket)
                .slaPolicy(slaRule)
                .cycleNumber(0)
                .allocatedMinutes(slaRule.getDurationMinutes())
                .slaStartAt(slaStart)
                .originalDeadlineAt(deadline)
                .currentDeadlineAt(deadline)
                .warningAt(warningTime)
                .status(SlaStatus.RUNNING.name())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ticketSla = ticketSlaRepository.save(ticketSla);

        log.info(
                "Created SLA instance for ticket {} with deadline {}",
                ticket.getId(),
                deadline
        );

        return ticketSla;
    }

    @Override
    @Transactional
    public void updateSlaStatus(Ticket ticket) {
        TicketSla ticketSla = ticketSlaRepository.findByTicketId(ticket.getId());
        if (ticketSla == null) {
            return;
        }

        if (SlaStatus.PAUSED.name().equals(ticketSla.getStatus())) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        if (now.isAfter(ticketSla.getCurrentDeadlineAt())) {
            ticketSla.setStatus(SlaStatus.BREACHED.name());
            ticketSla.setBreachedAt(now);
            log.warn("SLA breached for ticket {}", ticket.getId());
        } else if (ticketSla.getWarningAt() != null && now.isAfter(ticketSla.getWarningAt())) {
            ticketSla.setStatus(SlaStatus.WARNING.name());
            log.info("SLA warning for ticket {}", ticket.getId());
        } else {
            ticketSla.setStatus(SlaStatus.RUNNING.name());
        }

        ticketSla.setUpdatedAt(now);
        ticketSlaRepository.save(ticketSla);
    }

    @Override
    @Transactional
    public void pauseSla(Long ticketId) {

        TicketSla ticketSla = ticketSlaRepository.findByTicketId(ticketId);

        if (ticketSla != null &&
                (SlaStatus.RUNNING.name().equals(ticketSla.getStatus())
                        || SlaStatus.WARNING.name().equals(ticketSla.getStatus()))) {

            LocalDateTime now = LocalDateTime.now();

            ticketSla.setStatus(SlaStatus.PAUSED.name());
            ticketSla.setPausedAt(now);
            ticketSla.setUpdatedAt(now);

            ticketSlaRepository.save(ticketSla);

            log.info("SLA paused for ticket {}", ticketId);
        }
    }

    @Override
    @Transactional
    public void resumeSla(Long ticketId) {
        TicketSla ticketSla = ticketSlaRepository.findByTicketId(ticketId);
        if (ticketSla != null && SlaStatus.PAUSED.name().equals(ticketSla.getStatus())) {
            int pausedDuration = businessTimeService.calculateWorkingMinutes(
                    ticketSla.getPausedAt(),
                    LocalDateTime.now()
            );

            LocalDateTime newDeadline = businessTimeService.addWorkingMinutes(
                    ticketSla.getCurrentDeadlineAt(),
                    pausedDuration
            );

            ticketSla.setCurrentDeadlineAt(newDeadline);
            if (ticketSla.getWarningAt() != null) {
                ticketSla.setWarningAt(businessTimeService.addWorkingMinutes(
                        ticketSla.getWarningAt(),
                        pausedDuration
                ));
            }

            ticketSla.setStatus(SlaStatus.RUNNING.name());
            ticketSla.setPausedAt(null);
            ticketSla.setUpdatedAt(LocalDateTime.now());
            ticketSlaRepository.save(ticketSla);
            log.info("Resumed SLA for ticket {} with new deadline {}", ticketId, newDeadline);
        }
    }

    @Override
    @Transactional
    public void completeSla(Long ticketId) {
        TicketSla ticketSla = ticketSlaRepository.findByTicketId(ticketId);
        if (ticketSla != null) {
            ticketSla.setStatus(SlaStatus.COMPLETED.name());
            ticketSla.setUpdatedAt(LocalDateTime.now());
            ticketSlaRepository.save(ticketSla);
            log.info("Completed SLA for ticket {}", ticketId);
        }
    }

    @Override
    @Transactional
    public void checkAndNotifySlaBreaches() {
        LocalDateTime now = LocalDateTime.now();

        // Check for breaches from both RUNNING and WARNING statuses
        List<TicketSla> runningBreachedSlas = ticketSlaRepository.findByStatusAndCurrentDeadlineAtBefore(
                SlaStatus.RUNNING.name(),
                now
        );

        List<TicketSla> warningBreachedSlas = ticketSlaRepository.findByStatusAndCurrentDeadlineAtBefore(
                SlaStatus.WARNING.name(),
                now
        );

        // Combine and process breaches
        List<TicketSla> allBreachedSlas = new java.util.ArrayList<>();
        allBreachedSlas.addAll(runningBreachedSlas);
        allBreachedSlas.addAll(warningBreachedSlas);

        for (TicketSla ticketSla : allBreachedSlas) {
            // Only mark as breached if not already breached
            if (!SlaStatus.BREACHED.name().equals(ticketSla.getStatus())) {
                ticketSla.setStatus(SlaStatus.BREACHED.name());
                ticketSla.setBreachedAt(now);
                ticketSla.setUpdatedAt(now);
                ticketSlaRepository.save(ticketSla);
                log.warn("SLA breach detected and marked for ticket {}", ticketSla.getTicket().getId());
            }
        }

        // Check for warnings (only from RUNNING to avoid re-marking)
        List<TicketSla> warningSlas = ticketSlaRepository.findByStatusAndWarningAtBefore(
                SlaStatus.RUNNING.name(),
                now
        );

        for (TicketSla ticketSla : warningSlas) {
            if (!SlaStatus.BREACHED.name().equals(ticketSla.getStatus())) {
                ticketSla.setStatus(SlaStatus.WARNING.name());
                ticketSla.setUpdatedAt(now);
                ticketSlaRepository.save(ticketSla);
                log.info("SLA warning detected and marked for ticket {}", ticketSla.getTicket().getId());
            }
        }
    }

    @Override
    @Transactional
    public TicketSla createSlaInstanceForReopen(Ticket ticket, Integer allocatedMinutes) {
        SlaRule slaRule = slaRuleRepository.findByDepartmentIdAndSubCategoryIdAndActiveTrue(
                ticket.getDepartment().getId(),
                ticket.getSubCategory().getId()
        );

        if (slaRule == null) {
            log.warn("No active SLA rule found for department {} and sub-category {}",
                    ticket.getDepartment().getId(),
                    ticket.getSubCategory().getId());
            return null;
        }

        // Get the current cycle number for this ticket
        List<TicketSla> existingSlas = ticketSlaRepository.findAll().stream()
                .filter(sla -> sla.getTicket().getId().equals(ticket.getId()))
                .toList();

        int nextCycleNumber = existingSlas.stream()
                .mapToInt(TicketSla::getCycleNumber)
                .max()
                .orElse(0) + 1;

        LocalDateTime slaStart = businessTimeService.getNextWorkingTime(LocalDateTime.now());
        LocalDateTime deadline = businessTimeService.addWorkingMinutes(slaStart, allocatedMinutes);

        LocalDateTime warningTime = slaRule.getWarningMinutes() != null
                ? businessTimeService.addWorkingMinutes(slaStart, allocatedMinutes - slaRule.getWarningMinutes())
                : null;

        TicketSla ticketSla = TicketSla.builder()
                .ticket(ticket)
                .slaPolicy(slaRule)
                .cycleNumber(nextCycleNumber)
                .allocatedMinutes(allocatedMinutes)
                .slaStartAt(slaStart)
                .originalDeadlineAt(deadline)
                .currentDeadlineAt(deadline)
                .warningAt(warningTime)
                .status(SlaStatus.RUNNING.name())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ticketSla = ticketSlaRepository.save(ticketSla);

        log.info("Created SLA instance for reopened ticket {} with cycle {} and deadline {}",
                ticket.getId(), nextCycleNumber, deadline);

        return ticketSla;
    }
}
