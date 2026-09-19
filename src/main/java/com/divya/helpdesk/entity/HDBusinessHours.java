package com.divya.helpdesk.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
@Table(name = "hd_business_hours",
        uniqueConstraints = {@UniqueConstraint(name = "uk_cal_day", columnNames = {"business_calendar_id", "day_of_week"})}
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HDBusinessHours extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_calendar_id", nullable = false)
    private HDBusinessCalendar businessCalendar;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 20)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "is_working_day", nullable = false)
    private Boolean isWorkingDay = true;
}
