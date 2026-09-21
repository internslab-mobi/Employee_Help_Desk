package com.divya.helpdesk.service.impl;

import com.divya.helpdesk.entity.*;
import com.divya.helpdesk.enums.HDSlaStatus;
import com.divya.helpdesk.repository.HDBusinessCalendarRepository;
import com.divya.helpdesk.repository.HDSlaInstanceRepository;
import com.divya.helpdesk.service.SlaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SlaServiceImpl implements SlaService {

    private final HDSlaInstanceRepository slaInstanceRepository;
    private final HDBusinessCalendarRepository calendarRepository;
    private final HDSlaCalculationService slaCalculationService;

    @Override
    public HDSlaInstance createSlaInstance(HDTicket ticket, HDSlaPolicy slaPolicy, LocalDateTime startTime) {
        if (slaPolicy == null) {
            return null;
        }

        LocalDateTime effectiveStart = startTime != null ? startTime : LocalDateTime.now();

        // Retrieve business calendar for ticket's department or system default
        HDBusinessCalendar calendar = null;
        if (ticket.getDepartment() != null && ticket.getDepartment().getBusinessCalendar() != null) {
            calendar = ticket.getDepartment().getBusinessCalendar();
        } else {
            calendar = calendarRepository.findByIsDefaultTrue().orElse(null);
        }

        // get duration or set default
        int durationMinutes = slaPolicy.getDurationMinutes() != null ? slaPolicy.getDurationMinutes() : 480;

        LocalDateTime deadline;
        LocalDateTime warningTime;

        if (calendar != null) {
            deadline = slaCalculationService.calculateDueAt(calendar, effectiveStart, durationMinutes);
            if (slaPolicy.getWarningMinutes() != null) {
                warningTime = slaCalculationService.calculateDueAt(calendar, effectiveStart, slaPolicy.getWarningMinutes());
            } else {
                warningTime = slaCalculationService.calculateWarningAt(calendar, effectiveStart, durationMinutes);
            }
        } else {
            deadline = effectiveStart.plusMinutes(durationMinutes);
            int warnMins = slaPolicy.getWarningMinutes() != null ? slaPolicy.getWarningMinutes() : (int) Math.ceil(durationMinutes * 0.75);
            warningTime = effectiveStart.plusMinutes(warnMins);
        }

        HDSlaInstance slaInstance = new HDSlaInstance();
        slaInstance.setTicket(ticket);
        slaInstance.setSlaPolicy(slaPolicy);
        slaInstance.setCycleNumber(1);
        slaInstance.setAllocatedMinutes(durationMinutes);
        slaInstance.setSlaStartAt(effectiveStart);
        slaInstance.setOriginalDeadlineAt(deadline);
        slaInstance.setCurrentDeadlineAt(deadline);
        slaInstance.setWarningAt(warningTime);
        slaInstance.setStatus(HDSlaStatus.ACTIVE);

        return slaInstanceRepository.save(slaInstance);
    }

    @Override
    public HDSlaInstance recalculateSlaInstance(HDSlaInstance slaInstance, HDTicket ticket, HDSlaPolicy slaPolicy, LocalDateTime startAt) {
        if (slaInstance == null) {
            throw new IllegalArgumentException("SLA instance cannot be null");
        }
        if (slaPolicy == null) {
            throw new IllegalArgumentException("SLA policy cannot be null");
        }
        LocalDateTime effectiveStart = startAt != null ? startAt : LocalDateTime.now();
        int durationMinutes = slaPolicy.getDurationMinutes() != null ? slaPolicy.getDurationMinutes() : 480;

        HDBusinessCalendar calendar = null;
        if (ticket.getDepartment() != null) {
            calendar = ticket.getDepartment().getBusinessCalendar();
        }
        if (calendar == null) {
            calendar = calendarRepository.findByIsDefaultTrue().orElse(null);
        }

        LocalDateTime deadline;
        LocalDateTime warningAt;
        if (calendar != null) {
            deadline = slaCalculationService.calculateDueAt(calendar, effectiveStart, durationMinutes);
            int warningMinutes = slaPolicy.getWarningMinutes() != null ? slaPolicy.getWarningMinutes() : (int) Math.ceil(durationMinutes * 0.75);
            warningAt = slaCalculationService.calculateDueAt(calendar, effectiveStart, warningMinutes);
        } else {
            deadline = effectiveStart.plusMinutes(durationMinutes);
            int warningMinutes = slaPolicy.getWarningMinutes() != null ? slaPolicy.getWarningMinutes() : (int) Math.ceil(durationMinutes * 0.75);
            warningAt = effectiveStart.plusMinutes(warningMinutes);
        }
        slaInstance.setSlaPolicy(slaPolicy);
        slaInstance.setAllocatedMinutes(durationMinutes);
        slaInstance.setSlaStartAt(effectiveStart);

        /*
         * New deadline because the ticket classification changed.
         */
        slaInstance.setOriginalDeadlineAt(deadline);
        slaInstance.setCurrentDeadlineAt(deadline);
        slaInstance.setWarningAt(warningAt);
        slaInstance.setPausedAt(null);
        slaInstance.setBreachedAt(null);
        slaInstance.setStatus(HDSlaStatus.ACTIVE);

        return slaInstanceRepository.save(slaInstance);
    }

    @Override
    public LocalDateTime calculateDueAt(HDBusinessCalendar calendar, LocalDateTime startDateTime, int resolutionMinutes) {
        if (calendar == null) {
            return startDateTime.plusMinutes(resolutionMinutes);
        }
        return slaCalculationService.calculateDueAt(calendar, startDateTime, resolutionMinutes);
    }

    @Override
    public LocalDateTime calculateWarningAt(HDBusinessCalendar calendar, LocalDateTime startDateTime, int resolutionMinutes) {
        if (calendar == null) {
            int warningMinutes = (int) Math.ceil(resolutionMinutes * 0.75);
            return startDateTime.plusMinutes(warningMinutes);
        }
        return slaCalculationService.calculateWarningAt(calendar, startDateTime, resolutionMinutes);
    }

    @Override
    @Transactional
    public void checkSlaInstances() {
        LocalDateTime now = LocalDateTime.now();
        List<HDSlaInstance> slaInstances = slaInstanceRepository.findByStatus(HDSlaStatus.ACTIVE);
        for (HDSlaInstance sla : slaInstances) {
            // 1. Check breach first
            if (!now.isBefore(sla.getCurrentDeadlineAt())) {
                if (sla.getBreachSentAt() == null) {
                    // We will send the breach email here sendBreachEmail(sla);
                    sla.setBreachSentAt(now);
                }
                sla.setStatus(HDSlaStatus.BREACHED);
                sla.setBreachedAt(now);
                slaInstanceRepository.save(sla);
                continue;
            }
            // 2. Check warning
            if (sla.getWarningAt() != null && !now.isBefore(sla.getWarningAt()) && sla.getWarningSentAt() == null) {
                // We will send the warning email here sendWarningEmail(sla);
                sla.setWarningSentAt(now);
                slaInstanceRepository.save(sla);
            }
        }
    }
}
