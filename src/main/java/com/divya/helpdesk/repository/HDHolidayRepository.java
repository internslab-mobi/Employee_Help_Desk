package com.divya.helpdesk.repository;

import com.divya.helpdesk.entity.HDHoliday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HDHolidayRepository extends JpaRepository<HDHoliday, Long> {
    List<HDHoliday> findByBusinessCalendarId(Long calendarId);
    Optional<HDHoliday> findByBusinessCalendarIdAndHolidayDate(Long calendarId, LocalDate holidayDate);
    boolean existsByBusinessCalendarIdAndHolidayDate(Long calendarId, LocalDate holidayDate);
    List<HDHoliday> findByBusinessCalendarIdAndHolidayDateBetween(Long calendarId, LocalDate startDate, LocalDate endDate);
    void deleteByBusinessCalendarId(Long calendarId);
}
