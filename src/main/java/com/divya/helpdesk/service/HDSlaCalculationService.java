package com.divya.helpdesk.service;

import com.divya.helpdesk.entity.HDSlaPolicy;

import java.time.Instant;

public interface HDSlaCalculationService {

    /**
     * Adds the specified working minutes to a start time based on the business calendar
     * working hours and configured holidays.
     */
    Instant addWorkingMinutes(Long calendarId, Instant start, int minutes);

    /**
     * Calculates the SLA resolution deadline for a given policy and start time.
     */
    Instant calculateDeadline(HDSlaPolicy policy, Instant startTime);

    /**
     * Calculates the SLA warning threshold time for a given policy and start time.
     */
    Instant calculateWarningTime(HDSlaPolicy policy, Instant startTime);

    /**
     * Calculates the SLA resolution deadline for reopened tickets (typically 50% allocation).
     */
    Instant calculateReopenDeadline(HDSlaPolicy policy, int cycleNumber, Instant startTime);
}
