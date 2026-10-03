package com.example.helpdesk.service.impl;

import com.example.helpdesk.config.HolidayConfig;
import com.example.helpdesk.service.WorkingHours;
import com.example.helpdesk.service.BusinessTimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;

@Service
@RequiredArgsConstructor
public class BusinessTimeServiceImpl implements BusinessTimeService {

    private final HolidayConfig holidayConfig;

    @Override
    public Instant addWorkingMinutes(Instant from, int minutes, ZoneId zoneId) {
        Instant current = from;
        int remainingMinutes = minutes;

        while (remainingMinutes > 0) {
            current = getNextWorkingTime(current, zoneId);

            if (!isWorkingDateTime(current, zoneId)) {
                current = getNextWorkingTime(current, zoneId);
                continue;
            }

            Instant endOfDay = getEndOfWorkingDay(current, zoneId);
            long minutesUntilEndOfDay = java.time.Duration.between(current, endOfDay).toMinutes();

            if (minutesUntilEndOfDay >= remainingMinutes) {
                return current.plusSeconds(remainingMinutes * 60L);
            } else {
                remainingMinutes -= minutesUntilEndOfDay;
                current = endOfDay.plusSeconds(60);
            }
        }

        return current;
    }

    @Override
    public int calculateWorkingMinutes(Instant from, Instant to, ZoneId zoneId) {
        if (from.isAfter(to)) {
            return 0;
        }

        int totalMinutes = 0;
        Instant current = from;

        while (current.isBefore(to)) {
            if (isWorkingDateTime(current, zoneId)) {
                totalMinutes++;
            }
            current = current.plusSeconds(60);
        }

        return totalMinutes;
    }

    @Override
    public boolean isWorkingDateTime(Instant instant, ZoneId zoneId) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, zoneId);
        LocalDate date = dateTime.toLocalDate();
        LocalTime time = dateTime.toLocalTime();

        if (isHoliday(date)) {
            return false;
        }

        DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return false;
        }

        return WorkingHours.isWithinWorkingHours(time);
    }

    @Override
    public Instant getNextWorkingTime(Instant instant, ZoneId zoneId) {
        Instant current = instant;

        while (!isWorkingDateTime(current, zoneId)) {
            current = current.plusSeconds(60);
        }

        return current;
    }

    @Override
    public boolean isWorkingDay(LocalDate date) {
        if (isHoliday(date)) {
            return false;
        }

        DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return false;
        }

        return true;
    }

    private boolean isHoliday(LocalDate date) {
        return holidayConfig.isHoliday(date);
    }

    private Instant getEndOfWorkingDay(Instant instant, ZoneId zoneId) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, zoneId);
        LocalDate date = dateTime.toLocalDate();
        LocalTime endTime = WorkingHours.END;
        return LocalDateTime.of(date, endTime).atZone(zoneId).toInstant();
    }
}




