package com.divya.helpdesk.repository;

import com.divya.helpdesk.entity.HDHolidayEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface HDHolidayRepository extends JpaRepository<HDHolidayEntity, Long> {

    boolean existsByCalendarIdAndHolidayDate(Long calendarId, LocalDate holidayDate);
}
