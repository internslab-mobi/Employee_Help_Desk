package xyz.mobi.employeehelpdesk.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import xyz.mobi.employeehelpdesk.entity.Holiday;
import xyz.mobi.employeehelpdesk.repository.HolidayRepository;
import xyz.mobi.employeehelpdesk.service.WorkingCalendarService;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkingCalendarServiceImpl
        implements WorkingCalendarService {

    @Value("${helpdesk.sla.work-start}")
    private LocalTime workStart;

    @Value("${helpdesk.sla.work-end}")
    private LocalTime workEnd;

    private final HolidayRepository holidayRepository;

    @Override
    public LocalDateTime moveToWorkingTime(
            LocalDateTime dateTime) {

        LocalDate startDate = dateTime.toLocalDate();

        Set<LocalDate> holidays =
                holidayRepository
                        .findByHolidayDateBetween(
                                startDate,
                                startDate.plusDays(30)
                        )
                        .stream()
                        .map(Holiday::getHolidayDate)
                        .collect(Collectors.toSet());

        return moveToWorkingTime(
                dateTime,
                holidays
        );
    }

    @Override
    public LocalDateTime addWorkingMinutes(
            LocalDateTime start,
            long workingMinutes) {

        if (workingMinutes <= 0) {
            return moveToWorkingTime(start);
        }

        LocalDateTime current = start;

        long remainingMinutes = workingMinutes;

        Set<LocalDate> holidays =
                holidayRepository
                        .findByHolidayDateBetween(
                                start.toLocalDate(),
                                start.toLocalDate().plusYears(1)
                        )
                        .stream()
                        .map(Holiday::getHolidayDate)
                        .collect(Collectors.toSet());

        while (remainingMinutes > 0) {

            current = moveToWorkingTime(
                    current,
                    holidays
            );

            long availableMinutesToday =
                    Duration.between(
                            current.toLocalTime(),
                            workEnd
                    ).toMinutes();

            long minutesToAdd =
                    Math.min(
                            remainingMinutes,
                            availableMinutesToday
                    );

            current = current.plusMinutes(minutesToAdd);

            remainingMinutes -= minutesToAdd;

            if (remainingMinutes > 0) {
                current = nextDayAtWorkStart(current);
            }
        }

        return current;
    }

    @Override
    public long calculateWorkingMinutes(
            LocalDateTime start,
            LocalDateTime end) {

        if (start == null || end == null || !end.isAfter(start)) {
            return 0;
        }

        LocalDate startDate = start.toLocalDate();
        LocalDate endDate = end.toLocalDate();

        Set<LocalDate> holidays =
                holidayRepository
                        .findByHolidayDateBetween(
                                startDate,
                                endDate.plusDays(30)
                        )
                        .stream()
                        .map(Holiday::getHolidayDate)
                        .collect(Collectors.toSet());

        LocalDateTime current = moveToWorkingTime(start, holidays);
        if (!end.isAfter(current)) {
            return 0;
        }

        long totalWorkingMinutes = 0;

        while (current.isBefore(end)) {
            current = moveToWorkingTime(current, holidays);
            if (!current.isBefore(end)) {
                break;
            }

            LocalDate currentDate = current.toLocalDate();
            LocalDateTime endOfWorkToday = LocalDateTime.of(currentDate, workEnd);

            LocalDateTime effectiveEnd = end.isBefore(endOfWorkToday) ? end : endOfWorkToday;

            if (effectiveEnd.isAfter(current)) {
                totalWorkingMinutes += Duration.between(current, effectiveEnd).toMinutes();
            }

            current = nextDayAtWorkStart(current);
        }

        return totalWorkingMinutes;
    }

    private boolean isWorkingDay(
            LocalDateTime dateTime) {

        DayOfWeek day = dateTime.getDayOfWeek();

        return day != DayOfWeek.SATURDAY
                && day != DayOfWeek.SUNDAY;
    }

    private LocalDateTime nextDayAtWorkStart(
            LocalDateTime dateTime) {

        return dateTime
                .plusDays(1)
                .with(workStart);
    }

    private LocalDateTime moveToWorkingTime(
            LocalDateTime dateTime,
            Set<LocalDate> holidays) {

        LocalDateTime current = dateTime;

        while (true) {

            LocalDate date = current.toLocalDate();
            LocalTime time = current.toLocalTime();

            // Weekend or holiday
            if (!isWorkingDay(current)
                    || holidays.contains(date)) {

                current = nextDayAtWorkStart(current);
                continue;
            }

            // Before working hours
            if (time.isBefore(workStart)) {

                return LocalDateTime.of(
                        date,
                        workStart
                );
            }

            // After working hours
            if (!time.isBefore(workEnd)) {

                current = nextDayAtWorkStart(current);
                continue;
            }

            // Inside working hours
            return current;
        }
    }
}