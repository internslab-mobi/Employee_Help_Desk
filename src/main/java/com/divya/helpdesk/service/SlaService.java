package com.divya.helpdesk.service;

import com.divya.helpdesk.entity.HDBusinessCalendar;
import com.divya.helpdesk.entity.HDSlaInstance;
import com.divya.helpdesk.entity.HDSlaPolicy;
import com.divya.helpdesk.entity.HDTicket;

import java.time.LocalDateTime;

public interface SlaService {
    HDSlaInstance createSlaInstance(HDTicket ticket, HDSlaPolicy slaPolicy, LocalDateTime startTime);
    HDSlaInstance recalculateSlaInstance(HDSlaInstance slaInstance, HDTicket ticket, HDSlaPolicy slaPolicy, LocalDateTime startAt);
    LocalDateTime calculateDueAt(HDBusinessCalendar calendar, LocalDateTime startDateTime, int resolutionMinutes);
    LocalDateTime calculateWarningAt(HDBusinessCalendar calendar, LocalDateTime startDateTime, int resolutionMinutes);
    void checkSlaInstances();
}
