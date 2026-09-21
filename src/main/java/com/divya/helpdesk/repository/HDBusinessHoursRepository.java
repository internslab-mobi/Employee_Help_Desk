package com.divya.helpdesk.repository;

import com.divya.helpdesk.entity.HDBusinessHours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

@Repository
public interface HDBusinessHoursRepository extends JpaRepository<HDBusinessHours, Long> {
    List<HDBusinessHours> findByBusinessCalendarId(Long calendarId);
    Optional<HDBusinessHours> findByBusinessCalendarIdAndDayOfWeek(Long calendarId, DayOfWeek dayOfWeek);
    boolean existsByBusinessCalendarIdAndDayOfWeek(Long calendarId, DayOfWeek dayOfWeek);
}
