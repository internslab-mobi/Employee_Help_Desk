package com.divya.helpdesk.repository;

import com.divya.helpdesk.entity.HDBusinessHoursEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.Optional;

@Repository
public interface HDBusinessHoursRepository extends JpaRepository<HDBusinessHoursEntity, Long> {

    Optional<HDBusinessHoursEntity> findByCalendarIdAndDayOfWeek(Long calendarId, DayOfWeek dayOfWeek);
}
