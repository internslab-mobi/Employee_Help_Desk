package com.example.helpdesk.service.impl;

import com.example.helpdesk.config.WorkingHoursConfig;
import com.example.helpdesk.service.BusinessTimeService;
import com.example.helpdesk.service.HolidayService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class BusinessTimeServiceImpl implements BusinessTimeService {

    private final HolidayService holidayService;

    @Override
    public LocalDateTime addWorkingMinutes(LocalDateTime from, int minutes) {
        LocalDateTime current = from;
        int remainingMinutes = minutes;

        while (remainingMinutes > 0) {
            current = getNextWorkingTime(current);
            
            if (!isWorkingDateTime(current)) {
                current = getNextWorkingTime(current);
                continue;
            }

            LocalDateTime endOfDay = getEndOfWorkingDay(current);
            long minutesUntilEndOfDay = java.time.Duration.between(current, endOfDay).toMinutes();

            if (minutesUntilEndOfDay >= remainingMinutes) {
                return current.plusMinutes(remainingMinutes);
            } else {
                remainingMinutes -= minutesUntilEndOfDay;
                current = endOfDay.plusMinutes(1);
            }
        }

        return current;
    }

    @Override
    public int calculateWorkingMinutes(LocalDateTime from, LocalDateTime to) {
        if (from.isAfter(to)) {
            return 0;
        }

        int totalMinutes = 0;
        LocalDateTime current = from;

        while (current.isBefore(to)) {
            if (isWorkingDateTime(current)) {
                totalMinutes++;
            }
            current = current.plusMinutes(1);
        }

        return totalMinutes;
    }

    @Override
    public boolean isWorkingDateTime(LocalDateTime dateTime) {
        LocalDate date = dateTime.toLocalDate();
        LocalTime time = dateTime.toLocalTime();

        if (holidayService.isHoliday(date)) {
            return false;
        }

        DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return false;
        }

        return WorkingHoursConfig.isWorkingHour(time);
    }

    @Override
    public LocalDateTime getNextWorkingTime(LocalDateTime dateTime) {
        LocalDateTime current = dateTime;

        while (!isWorkingDateTime(current)) {
            current = current.plusMinutes(1);
        }

        return current;
    }

    private LocalDateTime getEndOfWorkingDay(LocalDateTime dateTime) {
        LocalDate date = dateTime.toLocalDate();
        LocalTime endTime = WorkingHoursConfig.getWorkEndTime();
        return LocalDateTime.of(date, endTime);
    }
}
