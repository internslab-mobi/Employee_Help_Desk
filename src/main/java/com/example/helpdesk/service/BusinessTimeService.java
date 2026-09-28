package com.example.helpdesk.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public interface BusinessTimeService {

    Instant addWorkingMinutes(Instant from, int minutes, ZoneId zoneId);

    int calculateWorkingMinutes(Instant from, Instant to, ZoneId zoneId);

    boolean isWorkingDateTime(Instant instant, ZoneId zoneId);

    Instant getNextWorkingTime(Instant instant, ZoneId zoneId);

    boolean isWorkingDay(LocalDate date);
}

