package com.divya.helpdesk.repository;

import com.divya.helpdesk.entity.HDBusinessCalendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HDBusinessCalendarRepository extends JpaRepository<HDBusinessCalendar, Long> {
    Optional<HDBusinessCalendar> findByCode(String code);
    Optional<HDBusinessCalendar> findByIsDefaultTrue();
    boolean existsByCode(String code);
    List<HDBusinessCalendar> findByIsActiveTrue();
}
