package xyz.mobi.employeehelpdesk.service;

import java.time.Instant;
import java.time.ZoneId;

public interface WorkingCalendarService {

    Instant addWorkingMinutes(
            Instant start,
            long workingMinutes,
            Long departmentId,
            ZoneId departmentZone
    );

    Instant moveToWorkingTime(
            Instant instant,
            Long departmentId,
            ZoneId departmentZone
    );

    long calculateWorkingMinutes(
            Instant start,
            Instant end,
            Long departmentId,
            ZoneId departmentZone
    );
}