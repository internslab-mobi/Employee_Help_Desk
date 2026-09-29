package com.divya.helpdesk.service.impl;

import com.divya.helpdesk.entity.HDBusinessCalendar;
import com.divya.helpdesk.entity.HDBusinessHours;
import com.divya.helpdesk.entity.HDSlaPolicy;
import com.divya.helpdesk.repository.HDBusinessCalendarRepository;
import com.divya.helpdesk.repository.HDBusinessHoursRepository;
import com.divya.helpdesk.repository.HDHolidayRepository;
import com.divya.helpdesk.service.HDSlaCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class HDSlaCalculationServiceImpl implements HDSlaCalculationService {

    private static final int MAX_CALENDAR_LOOKAHEAD_DAYS = 730; // 2 years safety limit

    private final HDBusinessHoursRepository businessHoursRepository;
    private final HDHolidayRepository holidayRepository;
    private final HDBusinessCalendarRepository businessCalendarRepository;

    @Override
    public Instant addWorkingMinutes(Long calendarId, Instant start, int minutes) {
        if (calendarId == null) {
            throw new IllegalArgumentException("Calendar ID cannot be null for SLA calculation");
        }

        if (start == null) {
            start = Instant.now();
        }

        if (minutes <= 0) {
            return start;
        }

        ZoneId zoneId = getCalendarZone(calendarId);
        ZonedDateTime current = start.atZone(zoneId);
        int remainingMinutes = minutes;
        int daysChecked = 0;

        while (remainingMinutes > 0) {
            if (daysChecked++ > MAX_CALENDAR_LOOKAHEAD_DAYS) {
                throw new IllegalStateException("Unable to calculate SLA: No valid working business hours found within "
                        + MAX_CALENDAR_LOOKAHEAD_DAYS + " days for calendar ID: " + calendarId);
            }

            LocalDate currentDate = current.toLocalDate();

            // Skip holidays
            if (isHoliday(calendarId, currentDate)) {
                current = nextDay(current);
                continue;
            }
            // Get business hours
            HDBusinessHours hours = getWorkingHours(calendarId, current.getDayOfWeek());
            if (hours == null) {
                current = nextDay(current);
                continue;
            }

            ZonedDateTime startOfWork = ZonedDateTime.of(currentDate, hours.getStartTime(), zoneId);
            ZonedDateTime endOfWork = ZonedDateTime.of(currentDate, hours.getEndTime(), zoneId);

            if (current.isBefore(startOfWork)) {
                current = startOfWork;
            }

            if (!current.isBefore(endOfWork)) {
                current = nextDay(current);
                continue;
            }

            long availableMinutesToday = Duration.between(current, endOfWork).toMinutes();

            if (availableMinutesToday >= remainingMinutes) {
                return current.plusMinutes(remainingMinutes).toInstant();
            }

            remainingMinutes -= (int) availableMinutesToday;
            current = nextDay(current);
        }

        return current.toInstant();
    }

    // CALCULATE DEADLINE
    @Override
    public Instant calculateDeadline(HDSlaPolicy policy, Instant startTime) {
        if (policy == null || policy.getCalendar() == null) {
            throw new IllegalArgumentException("SLA policy and its calendar must not be null");
        }
        return addWorkingMinutes(
                policy.getCalendar().getId(),
                startTime != null ? startTime : Instant.now(),
                policy.getResolutionTimeMinutes()
        );
    }

    // CALCULATE WARNING TIME
    @Override
    public Instant calculateWarningTime(HDSlaPolicy policy, Instant startTime) {
        if (policy == null || policy.getCalendar() == null) {
            throw new IllegalArgumentException("SLA policy and its calendar must not be null");
        }
        int warningMinutes = policy.getWarningTimeMinutes() != null
                && policy.getWarningTimeMinutes() > 0 ? policy.getWarningTimeMinutes() : (int) Math.round(policy.getResolutionTimeMinutes() * 0.75);

        return addWorkingMinutes(
                policy.getCalendar().getId(),
                startTime != null ? startTime : Instant.now(),
                warningMinutes
        );
    }

    // CALCULATE REOPEN DEADLINE
    @Override
    public Instant calculateReopenDeadline(HDSlaPolicy policy, int cycleNumber, Instant startTime) {
        if (policy == null || policy.getCalendar() == null) {
            throw new IllegalArgumentException("SLA policy and its calendar must not be null");
        }
        // 50% of the standard resolution allocation for reopened tickets
        int reopenMinutes = Math.max(1, policy.getResolutionTimeMinutes() / 2);

        return addWorkingMinutes(
                policy.getCalendar().getId(),
                startTime != null ? startTime : Instant.now(),
                reopenMinutes
        );
    }

    // MOVE TO WORKING TIME
    private ZonedDateTime moveToWorkingTime(Long calendarId, ZonedDateTime dateTime) {
        ZoneId zoneId = getCalendarZone(calendarId);
        ZonedDateTime current = dateTime.withZoneSameInstant(zoneId);
        int daysChecked = 0;

        while (true) {
            if (daysChecked++ > MAX_CALENDAR_LOOKAHEAD_DAYS) {
                throw new IllegalStateException("No valid business working days configured in calendar id: " + calendarId);
            }

            LocalDate date = current.toLocalDate();

            if (isHoliday(calendarId, date)) {
                current = nextDay(current);
                continue;
            }

            HDBusinessHours hours = getWorkingHours(calendarId, current.getDayOfWeek());
            if (hours == null) {
                current = nextDay(current);
                continue;
            }

            ZonedDateTime startOfWork = ZonedDateTime.of(date, hours.getStartTime(), zoneId);
            ZonedDateTime endOfWork = ZonedDateTime.of(date, hours.getEndTime(), zoneId);

            if (current.isBefore(startOfWork)) {
                return startOfWork;
            }

            if (current.isBefore(endOfWork)) {
                return current;
            }

            // At or after end of work -> advance to next day
            current = nextDay(current);
        }
    }

    private boolean isHoliday(Long calendarId, LocalDate date) {
        return holidayRepository.existsByCalendar_IdAndHolidayDate(calendarId, date);
    }

    private HDBusinessHours getWorkingHours(Long calendarId, DayOfWeek dayOfWeek) {
        return businessHoursRepository.findByCalendar_IdAndDayOfWeek(calendarId, dayOfWeek)
                .filter(h -> Boolean.TRUE.equals(h.getWorkingDay())
                        && h.getStartTime() != null
                        && h.getEndTime() != null
                        && h.getStartTime().isBefore(h.getEndTime()))
                .orElse(null);
    }

    private ZonedDateTime nextDay(ZonedDateTime current) {

        return current.plusDays(1)
                .toLocalDate()
                .atStartOfDay(current.getZone());
    }

    private ZoneId getCalendarZone(Long calendarId) {
        HDBusinessCalendar calendar = businessCalendarRepository.findById(calendarId)
                        .orElseThrow(() -> new IllegalArgumentException("Business calendar not found: " + calendarId));
        return ZoneId.of(calendar.getTimezone());
    }
}
