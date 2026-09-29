package com.divya.helpdesk.repository;

import com.divya.helpdesk.entity.HDHoliday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface HDHolidayRepository extends JpaRepository<HDHoliday, Long> {

    boolean existsByCalendar_IdAndHolidayDate(Long calendarId, LocalDate holidayDate);
}
