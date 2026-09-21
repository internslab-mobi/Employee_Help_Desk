package xyz.mobi.employeehelpdesk.service;

import java.time.LocalDateTime;

public interface WorkingCalendarService {

    LocalDateTime addWorkingMinutes(
            LocalDateTime start,
            long workingMinutes
    );

    public LocalDateTime moveToWorkingTime(
            LocalDateTime dateTime);

    long calculateWorkingMinutes(
            LocalDateTime start,
            LocalDateTime end
    );
}