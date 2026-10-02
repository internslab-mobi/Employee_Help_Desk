package com.divya.helpdesk.service.impl;

import com.divya.helpdesk.entity.HDBusinessCalendarEntity;
import com.divya.helpdesk.entity.HDBusinessHoursEntity;
import com.divya.helpdesk.entity.HDSlaPolicyEntity;
import com.divya.helpdesk.repository.HDBusinessCalendarRepository;
import com.divya.helpdesk.repository.HDBusinessHoursRepository;
import com.divya.helpdesk.repository.HDHolidayRepository;
import com.divya.helpdesk.service.SlaCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlaCalculationServiceImpl implements SlaCalculationService {

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
            HDBusinessHoursEntity hours = getWorkingHours(calendarId, current.getDayOfWeek());
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

    @Override
    public int calculateWorkingMinutes(Long calendarId, Instant start, Instant end) {
        if (calendarId == null || start == null || end == null || !start.isBefore(end)) {
            return 0;
        }

        ZoneId zoneId = getCalendarZone(calendarId);
        ZonedDateTime current = start.atZone(zoneId);
        ZonedDateTime endZdt = end.atZone(zoneId);
        long totalWorkingMinutes = 0;
        int daysChecked = 0;

        while (current.isBefore(endZdt)) {
            if (daysChecked++ > MAX_CALENDAR_LOOKAHEAD_DAYS) {
                break;
            }

            LocalDate currentDate = current.toLocalDate();

            // Skip holidays
            if (isHoliday(calendarId, currentDate)) {
                current = nextDay(current);
                continue;
            }

            // Get business hours
            HDBusinessHoursEntity hours = getWorkingHours(calendarId, current.getDayOfWeek());
            if (hours == null) {
                current = nextDay(current);
                continue;
            }

            ZonedDateTime startOfWork = ZonedDateTime.of(currentDate, hours.getStartTime(), zoneId);
            ZonedDateTime endOfWork = ZonedDateTime.of(currentDate, hours.getEndTime(), zoneId);

            if (current.isBefore(startOfWork)) {
                current = startOfWork;
            }

            if (!current.isBefore(endOfWork) || !current.isBefore(endZdt)) {
                current = nextDay(current);
                continue;
            }

            ZonedDateTime segmentEnd = endZdt.isBefore(endOfWork) ? endZdt : endOfWork;
            if (current.isBefore(segmentEnd)) {
                totalWorkingMinutes += Duration.between(current, segmentEnd).toMinutes();
            }

            current = nextDay(current);
        }

        return (int) totalWorkingMinutes;
    }

    // CALCULATE DEADLINE
    @Override
    public Instant calculateDeadline(HDSlaPolicyEntity policy, Instant startTime) {
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
    public Instant calculateWarningTime(HDSlaPolicyEntity policy, Instant startTime) {
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
    public Instant calculateReopenDeadline(HDSlaPolicyEntity policy, int cycleNumber, Instant startTime) {
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

            HDBusinessHoursEntity hours = getWorkingHours(calendarId, current.getDayOfWeek());
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
        return holidayRepository.existsByCalendarIdAndHolidayDate(calendarId, date);
    }

    private HDBusinessHoursEntity getWorkingHours(Long calendarId, DayOfWeek dayOfWeek) {
        return businessHoursRepository.findByCalendarIdAndDayOfWeek(calendarId, dayOfWeek)
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
        HDBusinessCalendarEntity calendar = businessCalendarRepository.findById(calendarId)
                        .orElseThrow(() -> new IllegalArgumentException("Business calendar not found: " + calendarId));
        return ZoneId.of(calendar.getTimezone());
    }
}
