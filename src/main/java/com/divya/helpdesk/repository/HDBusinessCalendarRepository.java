package com.divya.helpdesk.repository;

import com.divya.helpdesk.entity.HDBusinessCalendarEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HDBusinessCalendarRepository extends JpaRepository<HDBusinessCalendarEntity, Long> {
}
