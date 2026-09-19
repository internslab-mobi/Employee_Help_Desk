package com.divya.helpdesk.service.impl;

import com.divya.helpdesk.entity.HDBusinessCalendar;
import com.divya.helpdesk.entity.HDBusinessHours;
import com.divya.helpdesk.repository.HDBusinessHoursRepository;
import com.divya.helpdesk.repository.HDHolidayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class HDSlaCalculationService {

    private final HDBusinessHoursRepository businessHoursRepository;
    private final HDHolidayRepository holidayRepository;

    public LocalDateTime calculateDueAt(HDBusinessCalendar calendar, LocalDateTime startDateTime, int resolutionMinutes) {
        if (resolutionMinutes <= 0) {
            throw new IllegalArgumentException("Resolution minutes must be greater than zero");
        }

        LocalDateTime current = moveToWorkingTime(calendar, startDateTime);
        int remainingMinutes = resolutionMinutes;

        while (remainingMinutes > 0) {
            HDBusinessHours businessHours = getBusinessHours(calendar.getCalendarId(), current.getDayOfWeek());
            LocalDateTime workingDayEnd = LocalDateTime.of(current.toLocalDate(), businessHours.getEndTime());
            long availableMinutes = Duration.between(current, workingDayEnd).toMinutes();

            if (availableMinutes >= remainingMinutes) {
                return current.plusMinutes(remainingMinutes);
            }

            remainingMinutes -= (int) availableMinutes;
            current = moveToWorkingTime(calendar, current.toLocalDate().plusDays(1).atStartOfDay());
        }

        return current;
    }

    private LocalDateTime moveToWorkingTime(HDBusinessCalendar calendar, LocalDateTime dateTime) {
        LocalDateTime current = dateTime;

        while (true) {
            LocalDate date = current.toLocalDate();

            if (isHoliday(calendar, date)) {
                current = date.plusDays(1).atStartOfDay();
                continue;
            }
            HDBusinessHours businessHours = getBusinessHours(calendar.getCalendarId(), current.getDayOfWeek());

            if (!Boolean.TRUE.equals(businessHours.getIsWorkingDay())) {
                current = date.plusDays(1).atStartOfDay();
                continue;
            }
            LocalTime startTime = businessHours.getStartTime();
            LocalTime endTime = businessHours.getEndTime();
            LocalDateTime workingStart = LocalDateTime.of(date, startTime);
            LocalDateTime workingEnd = LocalDateTime.of(date, endTime);

            if (current.isBefore(workingStart)) {
                return workingStart;
            }
            if (!current.isBefore(workingEnd)) {
                current = date.plusDays(1).atStartOfDay();
                continue;
            }

            return current;
        }
    }

    private HDBusinessHours getBusinessHours(Long calendarId, DayOfWeek dayOfWeek) {
        return businessHoursRepository.findByBusinessCalendarIdAndDayOfWeek(calendarId, dayOfWeek)
                .orElseThrow(() -> new IllegalStateException("Business hours not configured for " + dayOfWeek));
    }

    private boolean isHoliday(HDBusinessCalendar calendar, LocalDate date) {
        return holidayRepository.existsByBusinessCalendarIdAndHolidayDate(calendar.getCalendarId(), date);
    }

    public LocalDateTime calculateWarningAt(HDBusinessCalendar calendar, LocalDateTime startDateTime, int resolutionMinutes) {
        int warningMinutes = (int) Math.ceil(resolutionMinutes * 0.75);
        return calculateDueAt(calendar, startDateTime, warningMinutes);
    }
}
