package com.divya.helpdesk.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "hd_holidays",
        uniqueConstraints = {@UniqueConstraint(
                        name = "uk_holiday_calendar_date",
                        columnNames = {"calendar_id", "holiday_date"}
                )
        }
)
public class HDHolidayEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "calendar_id", nullable = false, foreignKey = @ForeignKey(name = "fk_holiday_calendar"))
    private HDBusinessCalendarEntity calendar;

    @Column(name = "holiday_date", nullable = false)
    private LocalDate holidayDate;

    @Column(name = "name", nullable = false, length = 150)
    private String name;
}