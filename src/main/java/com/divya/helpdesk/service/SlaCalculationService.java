package com.divya.helpdesk.service;

import com.divya.helpdesk.entity.HDSlaPolicyEntity;

import java.time.Instant;

public interface SlaCalculationService {

    /**
     * Adds the specified working minutes to a start time based on the business calendar
     * working hours and configured holidays.
     */
    Instant addWorkingMinutes(Long calendarId, Instant start, int minutes);

    /**
     * Calculates the working minutes elapsed between start and end based on the business calendar
     * working hours and configured holidays.
     */
    int calculateWorkingMinutes(Long calendarId, Instant start, Instant end);

    /**
     * Calculates the SLA resolution deadline for a given policy and start time.
     */
    Instant calculateDeadline(HDSlaPolicyEntity policy, Instant startTime);

    /**
     * Calculates the SLA warning threshold time for a given policy and start time.
     */
    Instant calculateWarningTime(HDSlaPolicyEntity policy, Instant startTime);

    /**
     * Calculates the SLA resolution deadline for reopened tickets (typically 50% allocation).
     */
    Instant calculateReopenDeadline(HDSlaPolicyEntity policy, int cycleNumber, Instant startTime);
}
