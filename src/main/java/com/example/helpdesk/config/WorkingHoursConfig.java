package com.example.helpdesk.config;

import org.springframework.context.annotation.Configuration;

import java.time.LocalTime;

@Configuration
public class WorkingHoursConfig {

    public static final LocalTime WORK_START_TIME = LocalTime.of(8, 30); // 8:30 AM
    public static final LocalTime WORK_END_TIME = LocalTime.of(17, 30); // 5:30 PM

    public static LocalTime getWorkStartTime() {
        return WORK_START_TIME;
    }

    public static LocalTime getWorkEndTime() {
        return WORK_END_TIME;
    }

    public static boolean isWorkingHour(LocalTime time) {
        return !time.isBefore(WORK_START_TIME) && !time.isAfter(WORK_END_TIME);
    }
}
