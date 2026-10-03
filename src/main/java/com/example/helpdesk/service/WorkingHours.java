package com.example.helpdesk.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

/**
 * Fixed business-time rules for the Help Desk.
 *
 * Working hours: Monday–Friday, 08:30–17:30.
 * This is a plain utility class — not a Spring bean — because
 * these are fixed business rules, not externalized configuration.
 */
public final class WorkingHours {

    public static final LocalTime START = LocalTime.of(8, 30);
    public static final LocalTime END = LocalTime.of(17, 30);
    public static final Set<DayOfWeek> WORKING_DAYS = Set.of(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
    );

    private WorkingHours() {
        // utility class
    }

    /**
     * Returns {@code true} if the given time falls within working hours (inclusive).
     */
    public static boolean isWithinWorkingHours(LocalTime time) {
        return !time.isBefore(START) && !time.isAfter(END);
    }

    /**
     * Returns {@code true} if the given date is a working weekday
     * (does not check holidays — that is the caller's responsibility).
     */
    public static boolean isWorkingWeekday(LocalDate date) {
        return WORKING_DAYS.contains(date.getDayOfWeek());
    }
}
