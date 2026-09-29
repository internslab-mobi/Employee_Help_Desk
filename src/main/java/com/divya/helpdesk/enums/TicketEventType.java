package com.divya.helpdesk.enums;

public enum TicketEventType {
    CREATED,
    ASSIGNED,
    REASSIGNED,
    STATUS_CHANGED,
    PRIORITY_CHANGED,
    SLA_STARTED,
    SLA_WARNING,
    SLA_BREACHED,
    REOPENED,
    WITHDRAWN,
    RESOLVED
}