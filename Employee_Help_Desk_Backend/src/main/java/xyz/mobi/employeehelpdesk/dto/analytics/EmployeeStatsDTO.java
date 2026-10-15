package xyz.mobi.employeehelpdesk.dto.analytics;

public record EmployeeStatsDTO(
        long totalEmployees,
        long activeEmployees,
        long inactiveEmployees,
        long onLeaveEmployees
) {
}
