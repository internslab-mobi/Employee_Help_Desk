package com.divya.helpdesk.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "hd_holidays",
        uniqueConstraints = {@UniqueConstraint(name = "uk_cal_holiday_date", columnNames = {"business_calendar_id", "holiday_date"})}
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HDHoliday extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_calendar_id", nullable = false)
    private HDBusinessCalendar businessCalendar;

    @Column(name = "holiday_date", nullable = false)
    private LocalDate holidayDate;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;
}
