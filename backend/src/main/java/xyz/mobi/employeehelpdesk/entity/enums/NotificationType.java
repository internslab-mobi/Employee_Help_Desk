package xyz.mobi.employeehelpdesk.entity.enums;

public enum NotificationType {

    TICKET_CREATED,
    TICKET_ASSIGNED,
    TICKET_REASSIGNED,

    NEW_MESSAGE,
    TICKET_ON_HOLD,
    TICKET_RESUMED,

    TICKET_RESOLVED,
    TICKET_REOPENED,
    TICKET_WITHDRAWN,

    SLA_WARNING,
    SLA_BREACHED,

    FEEDBACK_RECEIVED,
    ESCALATION
}