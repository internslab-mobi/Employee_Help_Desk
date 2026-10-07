package xyz.mobi.employeehelpdesk.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import xyz.mobi.employeehelpdesk.entity.Holiday;
import xyz.mobi.employeehelpdesk.exception.BadRequestException;
import xyz.mobi.employeehelpdesk.repository.HolidayRepository;
import xyz.mobi.employeehelpdesk.service.WorkingCalendarService;

import java.time.*;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkingCalendarServiceImpl implements WorkingCalendarService {

    @Value("${helpdesk.sla.work-start:08:30}")
    private LocalTime workStart;

    @Value("${helpdesk.sla.work-end:17:30}")
    private LocalTime workEnd;

    private final HolidayRepository holidayRepository;

    @Override
    public Instant moveToWorkingTime(
            Instant instant,
            Long departmentId,
            ZoneId departmentZone
    ) {
        if (instant == null) {
            return null;
        }
        if (departmentZone == null) {
            throw new BadRequestException("Department ZoneId must not be null");
        }

        ZonedDateTime zdt = instant.atZone(departmentZone);
        LocalDate startDate = zdt.toLocalDate();

        Set<LocalDate> holidays = fetchHolidays(departmentId, startDate, startDate.plusDays(30));

        ZonedDateTime workingZdt = moveToWorkingTimeInternal(zdt, holidays, departmentZone);
        return workingZdt.toInstant();
    }

    @Override
    public Instant addWorkingMinutes(
            Instant start,
            long workingMinutes,
            Long departmentId,
            ZoneId departmentZone
    ) {
        if (start == null) {
            return null;
        }
        if (departmentZone == null) {
            throw new BadRequestException("Department ZoneId must not be null");
        }

        if (workingMinutes <= 0) {
            return moveToWorkingTime(start, departmentId, departmentZone);
        }

        ZonedDateTime current = start.atZone(departmentZone);
        LocalDate startDate = current.toLocalDate();

        Set<LocalDate> holidays = fetchHolidays(departmentId, startDate, startDate.plusYears(1));

        long remainingMinutes = workingMinutes;

        while (remainingMinutes > 0) {
            current = moveToWorkingTimeInternal(current, holidays, departmentZone);

            long availableMinutesToday =
                    Duration.between(
                            current.toLocalTime(),
                            workEnd
                    ).toMinutes();

            long minutesToAdd = Math.min(remainingMinutes, availableMinutesToday);

            current = current.plusMinutes(minutesToAdd);
            remainingMinutes -= minutesToAdd;

            if (remainingMinutes > 0) {
                current = nextDayAtWorkStart(current, departmentZone);
            }
        }

        return current.toInstant();
    }

    @Override
    public long calculateWorkingMinutes(
            Instant start,
            Instant end,
            Long departmentId,
            ZoneId departmentZone
    ) {
        if (start == null || end == null || !end.isAfter(start)) {
            return 0;
        }
        if (departmentZone == null) {
            throw new BadRequestException("Department ZoneId must not be null");
        }

        ZonedDateTime startZdt = start.atZone(departmentZone);
        ZonedDateTime endZdt = end.atZone(departmentZone);

        LocalDate startDate = startZdt.toLocalDate();
        LocalDate endDate = endZdt.toLocalDate();

        Set<LocalDate> holidays = fetchHolidays(departmentId, startDate, endDate.plusDays(30));

        ZonedDateTime current = moveToWorkingTimeInternal(startZdt, holidays, departmentZone);
        if (!endZdt.isAfter(current)) {
            return 0;
        }

        long totalWorkingMinutes = 0;

        while (current.isBefore(endZdt)) {
            current = moveToWorkingTimeInternal(current, holidays, departmentZone);
            if (!current.isBefore(endZdt)) {
                break;
            }

            LocalDate currentDate = current.toLocalDate();
            ZonedDateTime endOfWorkToday = currentDate.atTime(workEnd).atZone(departmentZone);

            ZonedDateTime effectiveEnd = endZdt.isBefore(endOfWorkToday) ? endZdt : endOfWorkToday;

            if (effectiveEnd.isAfter(current)) {
                totalWorkingMinutes += Duration.between(current, effectiveEnd).toMinutes();
            }

            current = nextDayAtWorkStart(current, departmentZone);
        }

        return totalWorkingMinutes;
    }

    private Set<LocalDate> fetchHolidays(Long departmentId, LocalDate from, LocalDate to) {
        if (departmentId == null || holidayRepository == null) {
            return Collections.emptySet();
        }
        return holidayRepository
                .findByDepartmentIdAndHolidayDateBetween(departmentId, from, to)
                .stream()
                .map(Holiday::getHolidayDate)
                .collect(Collectors.toSet());
    }

    private boolean isWorkingDay(ZonedDateTime dateTime) {
        DayOfWeek day = dateTime.getDayOfWeek();
        return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
    }

    private ZonedDateTime nextDayAtWorkStart(ZonedDateTime dateTime, ZoneId zone) {
        LocalDate nextDate = dateTime.toLocalDate().plusDays(1);
        return nextDate.atTime(workStart).atZone(zone);
    }

    private ZonedDateTime moveToWorkingTimeInternal(
            ZonedDateTime dateTime,
            Set<LocalDate> holidays,
            ZoneId zone
    ) {
        ZonedDateTime current = dateTime;

        while (true) {
            LocalDate date = current.toLocalDate();
            LocalTime time = current.toLocalTime();

            // Weekend or holiday in department local calendar
            if (!isWorkingDay(current) || holidays.contains(date)) {
                current = nextDayAtWorkStart(current, zone);
                continue;
            }

            // Before working hours
            if (time.isBefore(workStart)) {
                return current.toLocalDate().atTime(workStart).atZone(zone);
            }

            // After working hours
            if (!time.isBefore(workEnd)) {
                current = nextDayAtWorkStart(current, zone);
                continue;
            }

            // Inside working hours
            return current;
        }
    }
}