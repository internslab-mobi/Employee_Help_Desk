package com.divya.helpdesk.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;

@Getter
@Setter
@Entity
@Table(
        name = "hd_business_hours",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_calendar_day",
                        columnNames = {"calendar_id", "day_of_week"}
                )
        }
)
public class HDBusinessHours extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "calendar_id", nullable = false)
    private HDBusinessCalendar calendar;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 20)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "is_working_day", nullable = false)
    private Boolean workingDay;
}